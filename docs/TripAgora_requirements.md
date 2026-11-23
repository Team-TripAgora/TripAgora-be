# Trip Agora 프로젝트 기능 명세서 (화면 별 기능 + 시스템 엔진)

| 항목 | 내용 |
| --- | --- |
| 기준 | 백엔드 소스코드(`TripAgora-be`) |
| API 규모 | REST 56개 (12개 컨트롤러) + STOMP 메시지 핸들러 2개 (`StompController`) |
| 응답 규격 | 전 API 공통 `ApiResponse<T>` 봉투: `{ code, message, data?, errors? }`. HTTP 상태는 `code`와 같다. 검증 실패 시 필드별 오류 목록(`errors`) 포함 |
| 성공 상태 | 새 데이터를 만드는 POST 6개(여행·템플릿·리뷰·공지 생성, 참가 신청, 찜 추가)는 **201**, 나머지 성공은 **200** |
| 표기 | 화면은 앱의 화면 이름으로 적는다. 인증이 필요 없는 API는 별도 표기, 나머지는 전부 JWT 필요 |

---

## 0. 진입·로그인·온보딩 화면

### FEAT-AUTH-001 · 소셜 로그인 (카카오)

- **화면**: 로그인
- **API**: `POST /api/auth/login/{socialType}` (`KAKAO`만 받는다. 다른 값은 400, 인증 불필요)
- **정상 흐름**: 앱이 소셜 SDK로 받은 Access Token을 서버에 전달 → 서버가 카카오 사용자 정보 API를 직접 호출해 토큰을 검증 → `(socialId, socialType)`으로 회원 조회, 없으면 생성(이때 소셜 프로필 이미지를 S3로 다시 올려 자체 URL로 보관) → JWT Access/Refresh 발급, Refresh는 DB에 저장 → `isNewUser` 플래그로 앱이 온보딩 진입 여부를 결정
- 소셜 API 호출과 이미지 업로드는 DB 트랜잭션 밖에서 하고, 회원 저장이 실패하면 올린 이미지를 지운다
- **예외**: 소셜 토큰 무효(`SocialAccessTokenInvalidException`), 사용자 정보 파싱 실패(`SocialJsonParseErrorException`)
- 이메일/비밀번호 저장 없음 — 자체 자격증명을 아예 보유하지 않는 구조

### FEAT-AUTH-002 · 토큰 재발급 / 로그아웃 / 탈퇴

- **API**: `POST /api/auth/reissue` (인증 불필요) · `POST /api/auth/logout` · `DELETE /api/users/me`
- **재발급**: Refresh Token이 ① DB에 있고 ② 서명·타입(refresh)이 유효할 때만 새 쌍 발급. 발급할 때 기존 행을 지우고 새로 저장(rotate)하므로 쓰인 옛 토큰은 다시 쓸 수 없다. 토큰마다 고유 id(`jti`)가 있어 같은 초에 연달아 재발급해도 새 토큰이 나온다
- **로그아웃**: Refresh Token 삭제, 이 계정의 FCM 토큰 삭제(로그아웃한 기기로 SOS가 가지 않게)
- **탈퇴**: 모집·마감·진행 중인 여행에 참가 중이면 거절. 계정 행은 지우지 않고 개인정보를 비운다(소셜 id 무효화, 닉네임·프로필 이미지·FCM 토큰·관심사·찜 삭제, 가이드 프로필의 소개·이미지·외부 링크 삭제). Refresh Token과 위치 기록은 지운다. 프로필 이미지 파일은 DB 커밋 뒤 S3에서 지운다 — 다른 사람의 참가·리뷰·채팅 이력은 그대로 남는다

### FEAT-ONB-001 · 프로필 설정 (닉네임)

- **화면**: 프로필 설정
- **API**: `PATCH /api/users/me/nickname`, `PATCH /api/users/me/profile-image` (multipart)
- **검증**: 공백·제어문자 포함 금지(`InvalidNicknameFormatException`), 다른 사람과 중복 금지(`DuplicateNicknameException`, DB unique로 동시 요청도 막음). 자기 지금 닉네임으로 다시 저장하는 것은 허용
- 프로필 이미지 교체: 새 파일을 올린 뒤 DB를 바꾸고, 옛 파일은 DB 커밋 뒤 S3에서 지운다
- **내 정보 조회**: `GET /api/users/me` — 닉네임·현재 역할·프로필 이미지·관심사 태그 id를 돌려준다 (마이페이지)

### FEAT-ONB-002 · 관심사 태그 설정

- **화면**: 관심사 선택, 마이페이지 > 관심사 수정
- **API**: `PATCH /api/users/me/tags`
- **검증**: 최소 3개(`MinimumTagsRequiredException`), 존재하는 태그만(`InvalidTagSelectionException`). 갱신은 전체 교체

---

## 1. 공통 — 역할 전환

### FEAT-ROLE-001 · 여행자 ↔ 가이드 전환

- **화면**: 마이페이지 (양쪽 모드)
- **API**: `POST /api/users/me/switch-to-guide` · `POST /api/users/me/switch-to-traveler`
- **정상 흐름**: 역할 확인(이미 그 역할이면 `AlreadyGuideException`/`AlreadyTravelerException`) → 처음 가이드로 전환하면 가이드 프로필 생성(다시 전환할 때는 기존 프로필 재사용) → `users.role` 갱신 → **JWT 쌍을 새 role 클레임으로 재발급 + Refresh rotate**
- 권한의 근거가 토큰 클레임이므로 전환 즉시 새 토큰을 내려 앱이 교체하게 한다. 이후 요청은 Security의 URL 역할 규칙을 새 역할로 통과한다. 여행자로 돌아간 뒤 남아 있는 예전 가이드 토큰은 거부된다(SYS-001)

---

## 2. 여행자(TRAVELER) 화면

탭 구성: 홈 · 탐색 · 내 여행 · 위시리스트 · 마이페이지

### FEAT-EXP-001 · 여행 탐색/검색

- **화면**: 탐색
- **API**: `GET /api/trips/search?keyword&searchStartDate&searchEndDate&regionIds&tagIds&page&size` (Slice, 기본 10건)
- **동작**: **모집 중이고 시작일이 오늘보다 뒤인 여행**만 대상. 키워드(제목/소개/가이드 닉네임) · 날짜 구간 겹침 · 지역(부모 선택 시 자식 포함) · 태그 필터를 QueryDSL 동적 쿼리로 조합. 정렬은 시작일 → id 순 → 상세는 SYS-005

### FEAT-EXP-002 · 여행 상세 / 일정 보기

- **화면**: 여행 상세, 일정 상세
- **API**: `GET /api/trips/{tripId}` · `GET /api/trips/{tripId}/itineraries`
- **응답 내용**: 제목·소개·사진·지역·태그는 **여행이 열린 시점의 사본**, 가이드 정보는 최신
- **개인화 플래그**: `isParticipating`(참가 여부), `isMyTrip`(내가 가이드인지), `isInWishlist`, `hasWrittenReview`, `reviewId`(작성한 경우), `roomId`(모집 마감 전 null) — 앱 버튼 상태를 서버가 한 응답으로 결정

### FEAT-EXP-003 · 가이드 프로필 열람

- **화면**: 가이드 프로필
- **API**: `GET /api/guide-profiles/{guideProfileId}` · `GET /api/reviews/guide/{guideProfileId}` (Slice, 기본 20건)
- **내용**: 닉네임·소개·대표 이미지·관심사 태그·외부 링크(SNS/웹사이트)·모집 중 여행 목록·평점(totalAvgRating)·리뷰 목록

### FEAT-TRIP-001 · 참가 신청 / 취소

- **화면**: 여행 상세 하단 버튼
- **API**: `POST /api/participation/trips/{tripId}` (201) · `DELETE /api/participation/trips/{tripId}`
- **신청 검증**: 모집 중이고 시작 전날까지(`TRIP_NOT_RECRUITING` 400) / 본인 여행 금지(400) / 중복 참가 금지(`ALREADY_PARTICIPATING` 409) / **내 다른 완료되지 않은 여행과 기간 겹침 금지**(400) / 정원(`TRIP_FULL` 409)
- **동시성**: 신청자 행 → 여행 행 순서로 잠근 뒤 확인하고, (사용자, 여행) unique 제약을 둔다. 동시에 몰려도 정원 초과·중복 참가가 생기지 않는다 → 흐름 문서 §5
- **취소**: 가이드는 자기 여행의 참가를 취소할 수 없다(400). 모집 중이면 참가 기록 삭제·인원 감소, **완료된 여행이면 기록은 남기고 내 목록에서만 숨긴다**, 그 밖의 상태는 400

### FEAT-TRIP-002 · 내 여행 목록

- **화면**: 내 여행
- **API**: `GET /api/trips/participating?statuses=` (여행자로 참가한 여행, 숨긴 것 제외) · `GET /api/trips/completed` (완료 여행, 리뷰 작성 여부·리뷰 id 포함)

### FEAT-TRIP-003 · 그룹 채팅

- **화면**: 여행 방 > 채팅 (가이드 측 동일)
- **연결**: WebSocket `/connect` (SockJS) → STOMP CONNECT(JWT) → `SUBSCRIBE /topic/room/{roomId}/chat` → `SEND /publish/room/{roomId}/chat`
- **메시지**: 내용 필수, 1000자 이하. 발신 시점의 여행 내 역할(`senderRole`)이 실려 가이드 발언이 구분 표시된다
- **이력**: `GET /api/rooms/{roomId}/messages` (Slice, 기본 30건, 최신순) — 참가자만(아니면 403)
- 처리 중 오류(권한·검증)는 보낸 사람에게만 `/user/queue/errors`로 → SYS-002

### FEAT-TRIP-004 · 실시간 위치 공유

- **화면**: 여행 방 > 위치 보기 (가이드 측 동일)
- **연결**: `SEND /publish/room/{roomId}/location` (위도 −90~90, 경도 −180~180 필수) → 서버가 (사용자, 방)의 최신 위치를 저장·갱신한 뒤 `/topic/room/{roomId}/location`으로 브로드캐스트
- **초기화**: 방 입장 시 `GET /api/rooms/{roomId}/locations`로 전원의 마지막 위치를 받아 지도 마커 초기화 — 참가자만

### FEAT-TRIP-005 · SOS 긴급 호출

- **화면**: 여행 방 SOS 버튼
- **API**: `POST /api/rooms/{roomId}/sos` · FCM 토큰 등록 `POST /api/users/me/fcm-token`
- **조건**: 그 여행의 참가자(아니면 403)이고 여행이 **진행 중**(아니면 `TRIP_NOT_IN_PROGRESS` 400)
- **동작**: 보낸 사람을 뺀 같은 여행 참가자 중 FCM 토큰이 있는 사람에게 멀티캐스트 푸시("🚨 긴급 SOS 요청"). 페이로드에 `type=SOS`, `tripId`. 앱이 백그라운드여도 닿아야 하므로 STOMP가 아닌 FCM. 푸시 발송은 DB 트랜잭션 밖에서 한다
- **토큰**: 같은 기기 토큰이 다른 계정으로 등록되면 이전 계정에서는 지운다(마지막 로그인 계정에만 SOS가 간다)

### FEAT-TRIP-006 · 공지 열람

- **화면**: 여행 방 > 공지
- **API**: `GET /api/rooms/{roomId}/notices` (Slice, 기본 10건) · `GET /api/rooms/{roomId}/notices/{noticeId}` — 참가자만(아니면 403)

### FEAT-WISH-001 · 위시리스트

- **화면**: 위시리스트
- **API**: `POST /api/wishlist/{tripId}` (201) · `DELETE /api/wishlist/{tripId}` · `GET /api/wishlist` (Slice, 기본 10건)
- 찜 대상은 여행(날짜가 정해진 모집 건). 중복 추가는 `ALREADY_IN_WISHLIST` 409, (사용자, 여행) unique 제약으로 동시 요청도 막는다

### FEAT-REV-001 · 리뷰 작성/수정/삭제

- **화면**: 리뷰 작성 (완료된 여행 목록에서 진입)
- **API**: `POST /api/reviews` (201) · `PUT /api/reviews/{reviewId}` · `DELETE /api/reviews/{reviewId}` · 조회 `GET /api/reviews/template/{templateId}`, `GET /api/reviews/guide/{guideProfileId}` (Slice, 기본 20건)
- **작성 조건**: 완료된 여행(`TRIP_NOT_COMPLETED` 400) + 그 여행의 여행자 참가자(`REVIEW_NOT_ALLOWED` 403) + 여행당 1건(`ALREADY_REVIEWED` 409, unique 제약). 별점 1~5 + 내용
- 리뷰의 여행 제목은 리뷰한 여행의 제목. 작성·수정·삭제마다 템플릿·가이드 평점을 집계 쿼리로 다시 계산 → 흐름 문서 §9

---

## 3. 가이드(GUIDE) 화면

탭 구성: 홈 · 여행 · 템플릿 · 프로필 · 마이페이지

### FEAT-GP-001 · 가이드 프로필 관리

- **화면**: 프로필
- **API**: `GET /api/guide-profiles/me` · `PATCH /api/guide-profiles/me/bio` · `PATCH /api/guide-profiles/me/image` (multipart) · `PUT /api/guide-profiles/me/portfolios`
- 수정 API는 가이드 역할만. 외부 링크는 타입(FACEBOOK/INSTAGRAM/TWITTER/WEBSITE/YOUTUBE) + URL 목록의 전체 교체(최대 5개)

### FEAT-TPL-001 · 템플릿 작성 (단계식)

- **화면**: 템플릿 목록, 템플릿 상세, 태그·지역·일정 편집
- **API**: `POST /api/templates`(빈 초안 생성, 201) → `PATCH .../title` · `.../content` · `.../images`(multipart) · `.../tags` · `.../regions` · `PUT .../itineraries` → 조회 `GET /api/templates/me`(Slice, 기본 10건), `GET /api/templates/{templateId}`, `GET /api/templates/{templateId}/itineraries` → 삭제 `DELETE /api/templates/{templateId}`
- **검증**: 소유자 확인(전 API) · 제목·소개 255자 이하 · 태그 최대 5개 · 지역 부모/자식 동시 선택 금지 · 일정 Day 1부터 연속
- **수정·삭제 제한**: 모집·마감·진행 중 여행이 있으면 수정 불가, 여행을 한 번이라도 연 템플릿은 삭제 불가(`TEMPLATE_HAS_ACTIVE_TRIPS` 409)
- **사진 교체**: 새 파일을 올린 뒤 DB를 바꾸고, 옛 파일은 DB 커밋 뒤 지운다. 단, 여행 사본이 아직 쓰는 파일은 지우지 않는다 → 흐름 문서 §3

### FEAT-OPS-001 · 여행 열기 (모집 시작)

- **화면**: 템플릿 상세 > 모집 시작
- **API**: `POST /api/trips` (201) — `templateId + startDate(오늘 이후) + maxParticipants(1 이상)`만 입력
- **서버 결정 사항**: 종료일 자동 계산(일정의 마지막 Day 기준) · 같은 가이드의 모집·마감·진행 중 여행과 기간 겹침 차단(`TRIP_DATE_CONFLICT` 400) · **템플릿의 일정과 상품 정보(제목·소개·사진·지역·태그)를 여행 사본으로 복사** · 가이드 참가(role=GUIDE) 자동 생성

### FEAT-OPS-002 · 여행 운영

- **화면**: 여행 목록(상태별), 여행 상세, 모집 정보 수정
- **API**: `GET /api/trips/me?statuses=` (Slice, 기본 10건) · `PATCH /api/trips/{tripId}` · `DELETE /api/trips/{tripId}` · `POST /api/trips/{tripId}/close`(모집 마감 → 방 생성)
- **규칙**: 수정·삭제는 모집 중에만. 정원은 현재 인원보다 줄일 수 없다(400). 시작일을 바꾸면 기간 겹침을 다시 확인. 마감할 때 시작일이 오늘이거나 지났으면 바로 진행 중으로, 종료일이 지났으면 거절(`TRIP_ALREADY_ENDED` 400)
- 수정·삭제·마감은 여행 행을 잠그고 시작해, 같은 순간의 참가 신청과 엇갈리지 않는다
- 가이드가 마감하지 않아도 **시작일 00:00에 자동으로 마감**된다(SYS-004)

### FEAT-OPS-003 · 여행 중 일정 변경

- **화면**: 여행 상세 > 일정 편집
- **API**: `GET /api/trips/{tripId}/itineraries` · `PUT /api/trips/{tripId}/itineraries`
- **규칙**: 그 여행의 가이드(role=GUIDE) + 진행 중에만. 템플릿 원본에는 영향 없음(여행 사본) — 우천 등 현장 변수로 인한 당일 일정 변경 대응

### FEAT-OPS-004 · 공지 작성/수정/삭제

- **화면**: 여행 방 > 공지 작성
- **API**: `POST /api/rooms/{roomId}/notices` (201) · `PATCH /api/rooms/{roomId}/notices/{noticeId}` · `DELETE /api/rooms/{roomId}/notices/{noticeId}`
- **인가**: 그 여행의 가이드(서비스, 방 참가 확인과 같은 경로) + GET 외 메서드 `hasRole("GUIDE")`(Security URL). 제목·내용 255자 이하

---

## 4. 시스템 엔진

### SYS-001 · 인증/인가 체계 (JWT + URL 역할 규칙)

- **토큰**: HS256 서명, 클레임 `userId · role · type(access/refresh) · jti`. 만료는 설정값(분 단위)
- **필터**: `JWTFilter`(OncePerRequestFilter)가 Bearer 토큰을 한 번 해석해 `userId`를 Principal로, `ROLE_{role}`을 권한으로 SecurityContext에 넣는다. 인증이 필요 없는 경로(로그인·재발급·`/actuator/health`·`/connect/**`)는 필터를 건너뛴다
- **예전 가이드 토큰 거부**: 토큰의 역할이 가이드면 DB의 현재 역할을 한 번 확인해, 여행자로 바꾼 뒤의 예전 가이드 토큰은 401로 거부한다(여행자 토큰은 확인하지 않음)
- **URL 규칙**: `/api/templates/**`는 가이드, 가이드 프로필 수정(`PATCH/PUT /api/guide-profiles/me/**`)은 가이드, `/api/trips/**`의 GET 외 메서드는 가이드, 공지의 GET 외 메서드는 가이드, 나머지는 인증만
- **HTTP 세션**: 쓰지 않는다(STATELESS). 인증 실패(401)는 `CustomAuthenticationEntryPoint`, 권한 없음(403)은 `CustomAccessDeniedHandler`가 공통 응답 형식으로 내려준다

### SYS-002 · STOMP 실시간 메시징 엔진

- 엔드포인트 `/connect`(SockJS) · 앱 prefix `/publish` · 내장 SimpleBroker `/topic`(채팅방)·`/queue`(사용자별 오류 큐) · 하트비트 10초
- **주소 규칙**은 한 곳(`RoomDestinations`)에 있다: 구독 `/topic/room/{roomId}/(chat|location)`, 발행 `/publish/room/{roomId}/(chat|location)`
- `StompAuthInterceptor`의 프레임별 인가: CONNECT(JWT) → SUBSCRIBE(주소 형식 + 그 여행 참가 확인, 내 오류 큐 `/user/queue/errors`는 로그인만) → SEND(주소 형식). 형식이 틀리면 `INVALID_DESTINATION`, 참가자가 아니면 권한 없음으로 끊는다
- 채널 2종: `room/{roomId}/chat`(저장 후 브로드캐스트), `room/{roomId}/location`(저장·갱신 후 브로드캐스트)
- SEND 처리 중 오류(검증 400, 권한 403, 그 밖 500)는 보낸 사람(그 연결)에게만 `/user/queue/errors`로 HTTP API와 같은 형식(`code`, `message`, `errors`)으로 보낸다
- 연결/해제는 `StompEventListener`가 로깅 → 상세는 흐름 문서 §7

### SYS-003 · FCM 푸시 발송 (SOS)

- Firebase Admin SDK, 서비스 계정 키는 배포 시 GitHub Secrets에서 주입
- `sendEachForMulticast`로 다건 발송, Android 알림 채널 설정 포함, data 페이로드 `type=SOS, tripId`
- 발송 실패는 로깅 후 무시 — SOS API 자체는 성공 응답 (푸시 실패가 요청 실패로 전파되지 않음)

### SYS-004 · 여행 상태 스케줄러

- `@EnableScheduling` + cron 3건 (Asia/Seoul, 날짜는 한국 시간 `Clock`)
  - 00:00 시작일이 오늘 이하인 모집 중 여행 **자동 마감**(여행마다 잠그고 다시 확인 → 진행 중 + 방 생성)
  - 00:01 시작일이 오늘 이하인 모집 마감 여행 → 진행 중
  - 00:03 종료일이 지난 진행 중 여행 → 완료
- "이하"로 잡아, 서버가 멈춰 하루를 건너뛰어도 다음 실행에서 따라잡는다 → 흐름 문서 §8

### SYS-005 · QueryDSL 동적 검색 엔진

- `TripRepositoryCustom/Impl` — 모집 중 + 시작일 > 오늘 · 키워드 3필드 OR · 날짜 구간 겹침 · 지역 계층(부모/자식) OR · 태그 서브쿼리를 `BooleanBuilder`로 조합
- 템플릿·가이드 프로필·사용자는 fetch join, 지역 조인으로 늘어나는 줄은 distinct. 정렬은 시작일 → id(같은 날짜끼리 페이지 경계 고정), `limit+1` Slice로 COUNT 쿼리 생략
- 목록의 사진·지역 같은 연관 데이터는 배치 로딩(`default_batch_fetch_size: 100`)으로 묶어서 불러온다
- 그 외 단순 조회는 Spring Data 파생 쿼리, 고정 조건 확인·집계는 JPQL `@Query`

### SYS-006 · 이미지 파이프라인 (S3)

- `ImageService` 단일 진입점: 업로드(UUID 파일명, `images/` 프리픽스) · URL 소스 업로드(소셜 프로필 이관용) · 삭제
- 업로드는 DB 트랜잭션 밖에서, 옛 파일 삭제는 DB 커밋 뒤에(`ImageDeletionEvent`, 롤백되면 지우지 않음). DB 저장이 실패하면 방금 올린 파일을 지운다. 삭제 실패는 경고 로그만
- content-type `image/*` 검증(`UnSupportedImageFormatException`). 파일 한도 10MB
- 사용처: 사용자 프로필, 가이드 대표 이미지, 템플릿 이미지(다건, 여행 사본과 파일 공유)

### SYS-007 · 공통 응답·예외 체계

- 응답 봉투 `ApiResponse<T>`: `code`(HTTP 상태값) · `message` · `data`(null 제외) · `errors`(필드 검증 실패 목록). HTTP 상태는 응답을 쓰기 직전 한 곳(`ApiResponseStatusAdvice`)에서 `code`로 정해진다
- 예외는 도메인별 ErrorCode enum + `ApplicationException` 상속 계층 → `GlobalExceptionHandler`가 일괄 변환
- 로그: 4xx(정상적인 거절·입력 오류)는 warn 한 줄, 5xx(서버 문제)만 error + 스택트레이스
- STOMP 경로의 연결·구독 오류는 인터셉터에서 프레임을 거부, 메시지 처리 오류는 오류 큐로(SYS-002)

---

## 5. 관련 문서

| 문서 | 내용 |
| --- | --- |
| `TripAgora_plan.md` | 서비스 기획서 (문제 정의, 핵심 정책, 기획 대비 구현 범위) |
| `TripAgora_trip_realtime_flow.md` | 여행 라이프사이클 × 실시간 통신 흐름 상세 |
| `TripAgora_ERD.md` | 전체 DB 스키마 |
| `diagrams/` | ERD·핵심 흐름·AWS 아키텍처 다이어그램 |
