# Trip Agora 여행 라이프사이클 × 실시간 통신 흐름 명세서

| 항목 | 내용 |
| --- | --- |
| 범위 | 템플릿 작성 → 여행 열기 → 참가 → 모집 마감(방 생성) → 실시간 채팅·위치공유·SOS → 자동 상태 전환 → 리뷰 |
| 기준 | `TripAgora-be` 소스코드 (이 문서의 규칙·예외·채널명은 코드에서 확인한 값) |
| 관련 문서 | DB: `TripAgora_ERD.md` · 기능 명세: `TripAgora_requirements.md` · 기획: `TripAgora_plan.md` · 다이어그램: `diagrams/` |

---

## 1. 설계 원칙

1. **템플릿과 여행의 분리** — 가이드가 만드는 것은 "여행 상품의 원형(템플릿)"이고, 여행자가 참가하는 것은 "날짜가 정해진 여행"이다. 여행을 열 때 일정과 상품 정보(제목·소개·사진·지역·태그)를 여행 사본으로 복사하므로, 원형 수정과 진행 중이거나 끝난 여행이 서로를 바꾸지 않는다. 기준은 "그 시점의 기록인가, 최신 정보인가" — 상품 정보는 사본, 가이드 같은 사람 정보는 최신을 참조한다.
2. **참가(Participation)가 곧 권한이다** — 채팅 구독, 위치 공유, 공지 열람, SOS 발송의 인가 기준은 전부 "이 여행의 참가자인가"이고, 한 곳(`RoomAccessValidator`)에서 확인한다. 가이드도 여행을 열 때 자동으로 참가자(role=GUIDE)가 되므로, 별도의 가이드 전용 인가 경로 없이 하나의 검사로 통일된다.
3. **방(Room)은 모집 마감의 산물** — 실시간 소통 채널은 참가자가 확정된 뒤에만 열린다. 모집 중에는 roomId가 null이고, 마감(가이드가 직접 또는 시작일 자동)과 동시에 방이 생겨 채팅·공지·위치공유가 활성화된다.
4. **실시간과 영속의 이원화** — 채팅·위치는 STOMP로 브로드캐스트하되 반드시 DB에 먼저 기록한다(채팅은 전체 이력, 위치는 최신 스냅샷). 재접속·늦은 입장 시 REST로 과거 상태를 복원할 수 있다.
5. **시간 전이의 자동화** — 여행 시작·종료는 사람의 액션이 아니라 날짜의 경과다. 스케줄러가 매일 자정에 상태를 일괄 전환하고, 리뷰 권한 등 후속 규칙이 상태에 연동된다. '오늘'은 한국 시간 `Clock` 하나로 계산한다.
6. **바꾸는 작업은 잠그고 시작한다** — 여행 행의 인원·상태를 바꾸는 작업(참가 신청·취소, 정원 수정, 모집 마감, 삭제, 자동 마감)은 그 행을 `SELECT … FOR UPDATE`로 잠근 뒤 확인하고 바꾼다.

---

## 2. 전체 흐름도

```
[가이드]                                 [여행자]
   │                                        │
   ▼                                        │
① 템플릿 작성                               │
   POST /api/templates (빈 초안)            │
   → PATCH title/content/images/tags/regions│
   → PUT itineraries (Day별 일정)           │
   │                                        │
   ▼                                        ▼
② 여행 열기                           ③ 탐색·참가
   POST /api/trips                      GET /api/trips/search (QueryDSL, 시작일 > 오늘)
   · 일정·상품 정보 사본 복사            POST /api/participation/trips/{tripId}
   · endDate 자동 계산                   · 사용자 → 여행 행 잠금
   · 가이드 Participation 자동 생성      · 모집 기간/본인 여행/중복/일정 겹침/정원 확인
   │                                        │
   ▼                                        │
④ 모집 마감                                 │
   POST /api/trips/{tripId}/close (가이드)  │
   또는 시작일 00:00 자동 마감 (스케줄러)    │
   · RECRUITMENT_CLOSED 또는 IN_PROGRESS    │
   · Room 생성, trip.room_id ──┐            │
                               ▼            ▼
⑤ ═══════════ 실시간 구간 (방 단위) ═══════════
   WebSocket /connect (SockJS) · STOMP, 하트비트 10초
   CONNECT(JWT) → SUBSCRIBE(주소 형식 + 참가 확인) → SEND(주소 형식)
   · 채팅:  /publish/room/{roomId}/chat     → DB 저장 → /topic/room/{roomId}/chat
   · 위치:  /publish/room/{roomId}/location → 저장·갱신 → /topic/room/{roomId}/location
   · 오류:  보낸 사람에게만 /user/queue/errors
   · SOS:   POST /api/rooms/{roomId}/sos → FCM 멀티캐스트 (STOMP 아님, 진행 중에만)
   · 공지:  REST CRUD (가이드만 쓰기)
                               │
                               ▼
⑥ 자동 상태 전환 (TripScheduler, Asia/Seoul)
   00:00  RECRUITING + 시작일 ≤ 오늘          → 자동 마감 → IN_PROGRESS (방 생성)
   00:01  RECRUITMENT_CLOSED + 시작일 ≤ 오늘  → IN_PROGRESS
   00:03  IN_PROGRESS + 종료일 < 오늘          → COMPLETED
                               │
                               ▼
⑦ 리뷰  POST /api/reviews
   · COMPLETED + TRAVELER 참가자 + 미작성 건만
   · template.avgRating / guideProfile.totalAvgRating 집계 쿼리로 다시 계산
```

---

## 3. ① 템플릿 작성 — 빈 초안 + 필드별 갱신

`POST /api/templates`는 본문 없이 호출되어 **빈 초안 행**(title·content가 빈 문자열)을 만들고 ID만 돌려준다(201). 이후 앱의 단계별 작성 화면이 각 단계를 독립 API로 저장한다.

| 단계 | API | 검증 |
| --- | --- | --- |
| 제목 | `PATCH /api/templates/{templateId}/title` | 소유자 확인, 255자 이하 |
| 소개 | `PATCH /api/templates/{templateId}/content` | 소유자 확인, 255자 이하 |
| 이미지 | `PATCH /api/templates/{templateId}/images` (multipart) | 전체 교체, displayOrder 다시 부여 |
| 태그 | `PATCH /api/templates/{templateId}/tags` | 최대 5개(요청 검증), 존재하는 태그 ID만 |
| 지역 | `PATCH /api/templates/{templateId}/regions` | 존재하는 지역 ID만 + **부모·자식 동시 선택 금지** (`RegionHierarchyConflictException`) |
| 일정 | `PUT /api/templates/{templateId}/itineraries` | Day 번호 1부터 연속 (`ItineraryDaySequenceInvalidException`), 전체 교체 |

모든 수정 API는 공통으로 두 가지 가드를 통과해야 한다.

```java
// TemplateService.java
public Template findTemplateAndVerifyOwner(long userId, long templateId) { ... } // 소유자가 아니면 AccessDeniedException

private void verifyNoActiveTrips(long templateId) {
    if (tripRepository.existsActiveTripByTemplateId(templateId)) {
        throw new TemplateHasActiveTripsException();  // 모집·마감·진행 중 여행이 있으면 원형 수정 금지
    }
}
```

- 모집 중 여행은 템플릿과 같은 내용을 보여 주고 검색도 템플릿 기준이므로, 여행이 진행되는 동안에는 원형을 고칠 수 없다. 여행이 모두 끝난 템플릿은 고칠 수 있고, 끝난 여행은 자기 사본을 보여 주므로 바뀌지 않는다.
- 여행을 한 번이라도 연 템플릿은 삭제할 수 없다(그 여행의 참가·리뷰·채팅 이력을 지키기 위해).
- **이미지 교체와 파일 수명**: 새 파일은 DB 트랜잭션 밖에서 올리고, 옛 파일은 DB가 커밋된 뒤에 S3에서 지운다(롤백되면 지우지 않는다). 템플릿과 여행 사본은 같은 S3 파일을 함께 쓰므로, `trip_image`가 아직 쓰는 옛 파일은 지우지 않는다.

```java
// TemplateService.replaceImages()
eventPublisher.publishEvent(new ImageDeletionEvent(excludeUsedByTrips(oldImageUrls)));   // 커밋 뒤 삭제
```

---

## 4. ② 여행 열기

`POST /api/trips` — 요청은 `templateId · startDate · maxParticipants` 세 값뿐이다(201). 나머지는 서버가 결정한다.

```java
// TripService.createTrip()
LocalDate endDate = Trip.calculateEndDate(template, request.startDate());
// = startDate + (일정의 최대 Day - 1). 일정이 없으면 당일 여행

boolean hasConflict = tripRepository.existsConflictingTrip(
        template.getGuideProfile(), SCHEDULE_CONFLICT_STATUSES, request.startDate(), endDate);
if (hasConflict) throw new TripDateConflictException();   // 같은 가이드의 모집·마감·진행 중 여행과 기간 겹침 차단
```

여행을 만들면서 세 가지가 함께 일어난다.

1. **상품 정보 사본 복사** — `Trip` 생성자가 템플릿의 제목·소개를 컬럼으로, 이미지·태그·지역을 `trip_image`·`trip_tag`·`trip_region` 행으로 복사한다. 여행 상세·목록·완료 목록과 리뷰 제목은 이 사본을 읽는다. 가이드 정보(닉네임·사진·평점)는 최신을 참조한다.
2. **일정 사본 복사** — 템플릿 일정을 여행 일정(`itinerary` 테이블의 `trip_id` 행)으로 값 복사. 이후 템플릿과 여행의 일정은 서로 독립.
3. **가이드 Participation 자동 생성** — `role = GUIDE`. 이 한 행 덕분에 가이드는 이후 채팅·위치·공지·SOS의 참가자 확인을 그대로 통과한다.

여행 수정(`PATCH /api/trips/{tripId}`)·삭제(`DELETE`)는 `RECRUITING` 상태에서만, 여행 행을 잠근 뒤 허용된다. 수정 시 정원을 현재 참가 인원 아래로 줄일 수 없고(`InvalidMaxParticipantsException`), 시작일 변경 시 날짜 겹침을 다시 확인한다.

---

## 5. ③ 참가 — 즉시 확정 + 잠금 뒤 검증

참가는 승인 대기 없이 **즉시 확정**된다(201). 대신 신청자와 여행 행을 잠근 뒤 순서대로 확인한다.

```java
// ParticipationService.applyForTrip()
// 사용자 → 여행 순서로 잠근다 (순서를 고정해 데드락을 막는다)
User user = userRepository.findByIdForUpdate(userId)...;
Trip trip = tripRepository.findByIdForUpdate(tripId)...;

trip.validateJoinable(LocalDate.now(clock));               // 1. 모집 중이고 시작 전날까지 (TripNotRecruitingException)
if (trip.getTemplate().getGuideProfile().getUser().getId().equals(userId))
    throw new CannotParticipateInOwnTripException();        // 2. 본인 여행 참가 금지
participationRepository.findByUser_IdAndTrip_Id(userId, tripId)
    .ifPresent(p -> { throw new AlreadyParticipatingException(); });  // 3. 중복 참가 금지
boolean hasConflict = participationRepository.existsConflictingParticipation(
    userId, trip.getStartDate(), trip.getEndDate(), TripStatus.COMPLETED);
if (hasConflict) throw new ConflictingParticipationException();      // 4. 내 다른 여행과 기간 겹침 금지

trip.increaseParticipant();   // 5. 정원 초과 시 TripFullException
participationRepository.save(participation);   // (사용자, 여행) unique 위반이면 AlreadyParticipatingException
```

- **왜 잠그는가** — 잠그지 않으면 동시에 들어온 신청들이 같은 인원 수를 보고 모두 "자리 있음"으로 판단해 정원을 넘길 수 있다. 여행 행을 잠그면 같은 여행의 신청이 한 번에 하나씩 처리되고, 사용자 행을 잠그면 같은 사람이 날짜가 겹치는 여러 여행에 동시에 신청하는 것도 막힌다. 잠금은 트랜잭션의 첫 조회에서 잡아, 잠근 뒤의 확인이 앞 요청의 결과를 본다.
- **잠금 순서** — 신청·취소 모두 사용자 → 여행(취소는 여행만) 순서를 지켜 서로를 기다리는 원(데드락)이 생기지 않는다. 정원 수정·마감·삭제·자동 마감도 같은 여행 행 잠금을 쓴다.
- 4번은 JPQL로 "내가 참가 중인 미완료 여행 중 기간이 겹치는 것"을 조회한다 — 몸은 하나이므로 같은 기간에 두 여행을 갈 수 없다는 규칙이다.
- **취소**(`DELETE /api/participation/trips/{tripId}`) — 여행 행을 잠근 뒤, 가이드는 거절. 모집 중이면 참가 행 삭제 + `currentParticipants` 감소, 완료된 여행이면 행을 남기고 `hidden_at`만 채워 내 목록에서 숨긴다. 그 밖의 상태는 거절.

---

## 6. ④ 모집 마감과 방 생성

모집 마감이 여행 라이프사이클의 분기점이다. 두 경로가 있고, 둘 다 같은 방 생성 코드를 쓴다.

```java
// 가이드의 마감 — POST /api/trips/{tripId}/close
Trip trip = lockTripAndVerifyOwner(userId, tripId);     // 여행 행 잠금 + 소유자 확인
trip.closeRecruitment(LocalDate.now(clock));            // 종료일이 지났으면 TripAlreadyEndedException
// 시작일 전 → RECRUITMENT_CLOSED, 시작일 당일·이후 → 바로 IN_PROGRESS
openRoom(trip);

// 시작일 자동 마감 — 스케줄러가 여행마다 부른다 (여행 하나가 트랜잭션 하나)
Optional<Trip> locked = tripRepository.findByIdForUpdate(tripId);
if (locked.isEmpty() || locked.get().getStatus() != TripStatus.RECRUITING) return false;   // 그새 가이드가 마감·삭제
locked.get().closeRecruitmentOnStartDate(today);        // → IN_PROGRESS
openRoom(locked.get());

private void openRoom(Trip trip) {
    Room room = roomRepository.save(Room.create());
    trip.assignRoom(room);                               // trip.room_id가 방을 가리킨다
}
```

- 모집 중에는 `roomId = null` → 앱은 채팅·위치 탭을 비활성화한다.
- 마감과 동시에 방이 생기고, 참가자 전원(가이드 포함)이 같은 방의 실시간 채널에 접근할 수 있게 된다.
- 가이드의 마감과 자동 마감이 같은 순간에 돌아도, 잠금 뒤 상태를 다시 보므로 방은 하나만 생긴다.
- 방의 외래 키는 여행 쪽(`trip.room_id`)에 있다. 여행 목록을 읽을 때 방 번호가 외래 키 값으로 나와 방을 따로 조회하지 않는다.

---

## 7. ⑤ 실시간 구간 — STOMP 계약

### 7.1 연결 구성

| 항목 | 값 |
| --- | --- |
| WebSocket 엔드포인트 | `/connect` (SockJS 폴백 지원, Security 필터 제외 — 인증은 STOMP 레벨에서) |
| 애플리케이션 prefix | `/publish` |
| 브로커 | 내장 SimpleBroker, prefix `/topic`(채팅방) · `/queue`(사용자별 오류 큐). 외부 메시지 브로커 없음 — 단일 서버 구성 |
| 하트비트 | 서버 10초 (실제 간격은 앱이 요청한 값과 서버 값 중 큰 쪽), 끊긴 연결 감지 |
| 발행 주소 | `/publish/room/{roomId}/chat` · `/publish/room/{roomId}/location` |
| 구독 주소 | `/topic/room/{roomId}/chat` · `/topic/room/{roomId}/location` · `/user/queue/errors` |

주소 규칙은 한 곳(`RoomDestinations`)에 있다. 인터셉터는 이 규칙으로 검사하고, 컨트롤러는 보낼 주소를 여기서 만든다 — 채널을 바꿀 때 발행 쪽과 인가 쪽이 어긋나지 않게.

### 7.2 STOMP 프레임별 인가 — `StompAuthInterceptor`

HTTP 핸드셰이크는 열어 두고, STOMP 프레임 단계에서 인증·인가를 수행한다.

| 프레임 | 검사 | 실패 시 |
| --- | --- | --- |
| `CONNECT` | `Authorization: Bearer` 네이티브 헤더의 Access Token 검증 → `userId`·`role`로 Principal 구성 | `UnAuthorizedAccessException` / `AccessTokenInvalidException` |
| `SUBSCRIBE` | 내 오류 큐 `/user/queue/errors`면 로그인만 확인. 그 밖에는 주소가 `/topic/room/{roomId}/(chat\|location)` 형식인지 → **방 번호와 나로 참가 확인** | `InvalidDestinationException` / 권한 없음(`AccessDeniedException`) / 방 없음(`RoomNotFoundException`) |
| `SEND` | 인증 존재 + 주소가 `/publish/room/{roomId}/(chat\|location)` 형식인지 | `UnAuthorizedAccessException` / `InvalidDestinationException` |

참가 확인은 방 → 여행 → 참가를 따로 조회하지 않고 한 번에 한다.

```java
// RoomAccessValidator.validateParticipant() — 구독·채팅·위치·공지·SOS가 모두 이걸 쓴다
participationRepository.findByRoomIdAndUserId(roomId, userId)
        .orElseThrow(() -> roomRepository.existsById(roomId) ? new AccessDeniedException() : new RoomNotFoundException());

// ParticipationRepository — 방 번호는 여행의 외래 키, 사용자의 가이드 프로필까지 한 번에
@Query("SELECT p FROM Participation p JOIN FETCH p.user u LEFT JOIN FETCH u.guideProfile " +
       "JOIN FETCH p.trip t WHERE t.room.id = :roomId AND u.id = :userId")
```

구독 시점에 참가 여부를 DB로 확인하므로, 참가하지 않은 여행의 방 번호를 알아내도 채팅을 엿들을 수 없다. 참가자가 아니면 403, 방이 없으면 404다(REST도 같다).

### 7.3 채팅 — 저장 후 브로드캐스트

```java
// StompController.java
@MessageMapping("/room/{roomId}/" + RoomDestinations.CHAT)
public void sendMessage(@DestinationVariable Long roomId, @Valid @Payload ChatMessageRequest request, Principal principal) {
    Long userId = Long.valueOf(principal.getName());
    ChatMessageResponse response = chatService.processAndSaveMessage(userId, roomId, request); // ① 참가 확인 + DB 저장
    messagingTemplate.convertAndSend(RoomDestinations.topic(roomId, RoomDestinations.CHAT), response); // ② 브로드캐스트
}
```

- 메시지 내용은 필수, 1000자 이하(요청 검증). 처리는 참가 확인 조회 한 번 + 저장 한 번이다.
- 저장 시 발신자의 **여행 안 역할**(participation.role)을 `senderRole`로 남긴다 → 앱이 가이드 말풍선을 구분 표시.
- 브로드캐스트 페이로드에는 메시지 ID·발신자 닉네임·프로필 이미지·본문·시각·역할이 포함되어, 수신 측이 추가 조회 없이 바로 렌더링한다.
- 과거 이력: `GET /api/rooms/{roomId}/messages` (Slice 페이징, 최신순, 참가자만).

### 7.4 위치 공유 — 최신 스냅샷 저장·갱신

```java
// LocationService.updateLocation() — STOMP SEND 시 호출
Participation participation = roomAccessValidator.validateParticipant(userId, roomId);
UserLocation userLocation = userLocationRepository.findByUser_IdAndRoom_Id(userId, roomId)
        .orElseGet(() -> UserLocation.builder()... .build());   // 없으면 새로 저장, 있으면
userLocation.updateLocation(request.latitude(), request.longitude());  // 좌표만 갱신
```

- 위도 −90~90, 경도 −180~180 필수(요청 검증).
- 이력을 쌓지 않고 (사용자, 방)당 1행만 유지한다. 실시간 전파는 STOMP가 맡으므로 DB는 "지금 어디"만 답하면 된다.
- 방 입장 직후에는 `GET /api/rooms/{roomId}/locations`로 전원의 마지막 위치를 받아 지도를 초기화하고, 이후 브로드캐스트로 마커를 갱신한다.

### 7.5 메시지 처리 오류 — 보낸 사람에게만

```java
// StompController — 처리 중 오류는 보낸 그 연결에만 HTTP API와 같은 형식으로
@MessageExceptionHandler
@SendToUser(destinations = RoomDestinations.ERRORS, broadcast = false)   // /user/queue/errors
public ApiResponse<?> handleApplicationException(ApplicationException e) { return ApiResponse.fail(e.getErrorCode()); }
```

- 입력 검증 실패(400, 어느 칸이 왜 틀렸는지 `errors`), 권한(403), 읽을 수 없는 메시지(400), 그 밖의 오류(500)를 보낸 사람에게 알린다. 다른 참가자에게는 가지 않는다.
- 연결(CONNECT)·구독(SUBSCRIBE) 단계의 오류는 인터셉터가 프레임을 거부하고 연결을 끊는다(ERROR 프레임).

### 7.6 SOS — STOMP가 아니라 FCM인 이유

```java
// SosService.sendSos() — POST /api/rooms/{roomId}/sos, @Transactional 없음
Participation participation = roomAccessValidator.validateParticipant(userId, roomId);   // 참가자만 (403)
Trip trip = participation.getTrip();
trip.validateSosSendable();                                                               // 진행 중에만 (400)
List<String> targetTokens = participationRepository.findFcmTokensOfOtherParticipants(trip.getId(), userId);
fcmService.sendMulticastMessage(targetTokens, "🚨 긴급 SOS 요청",
        "'닉네임'님이 긴급 도움을 요청했습니다!", trip.getId());                         // 트랜잭션 밖에서 발송
```

SOS는 "앱을 보고 있지 않은 사람"에게 닿아야 하는 신호다. STOMP 브로드캐스트는 소켓이 연결된(= 화면을 보고 있는) 참가자에게만 전달되므로, SOS만은 OS 레벨 푸시(FCM `sendEachForMulticast`)로 보낸다.

- 받는 사람은 같은 여행 참가자 중 보낸 사람을 뺀, FCM 토큰이 있는 사람이다(쿼리 한 번).
- 페이로드에 `type: SOS`와 `tripId`를 실어 앱이 탭 시 그 여행 화면으로 이동할 수 있게 한다.
- FCM 토큰은 로그인 시 `POST /api/users/me/fcm-token`으로 등록·갱신된다. 같은 기기 토큰이 다른 계정으로 등록되면 이전 계정에서 지우고, 로그아웃하면 지운다 — 다른 사람의 SOS가 엉뚱한 기기로 가지 않게.
- 외부 호출(FCM) 동안 DB 커넥션을 잡지 않도록, 조회를 마친 뒤 트랜잭션 밖에서 발송한다. 발송 실패는 로깅만 하고 API는 성공으로 응답한다.

### 7.7 공지 — REST + 역할 검사

공지는 실시간성이 필요 없는 게시물이므로 REST CRUD로 처리한다 (`/api/rooms/{roomId}/notices`). 읽기는 참가자 전원, 쓰기(생성·수정·삭제)는 `RoomAccessValidator.validateGuide()`가 그 여행의 `role = GUIDE`를 확인한다. URL 레벨에서도 Security가 GET 외 메서드를 `hasRole("GUIDE")`로 막는다.

---

## 8. ⑥ 자동 상태 전환 — TripScheduler

```java
@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")   // 매일 00:00
public void closeStartedRecruitments() // RECRUITING + startDate ≤ 오늘 → 여행마다 잠그고 다시 확인 → IN_PROGRESS + 방 생성

@Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul")   // 매일 00:01
public void startTrips()               // RECRUITMENT_CLOSED + startDate ≤ 오늘 → IN_PROGRESS

@Scheduled(cron = "0 3 0 * * *", zone = "Asia/Seoul")   // 매일 00:03
public void completeTrips()            // IN_PROGRESS + endDate < 오늘 → COMPLETED
```

- 여행의 시작·종료는 날짜 경과라는 사실의 확인이므로 사용자 액션에 맡기지 않는다. 모집은 시작 전날까지이므로, 가이드가 마감하지 않은 여행도 시작일 00:00에 자동으로 마감되어 출발한다(신청자가 없어도).
- '오늘'은 한국 시간 `Clock`으로 계산한다. 조건을 "이하"로 잡아 서버가 멈춰 며칠을 건너뛰어도 다음 실행에서 따라잡는다 — 종료일까지 지난 모집 중 여행도 00:00에 진행 중이 되고 00:03에 완료된다.
- 완료 전이는 리뷰 작성 권한의 트리거이기도 하다 (COMPLETED 이후에만 리뷰 가능).

전체 상태 기계는 `TripAgora_ERD.md`의 TripStatus 전이 흐름 참조.

---

## 9. ⑦ 리뷰와 평점 재계산

`POST /api/reviews` 작성 조건 3종 (모두 서버 검증, 201):

1. 그 여행의 `TRAVELER` 참가자일 것 — 가이드 본인 리뷰 차단 (`ReviewNotAllowedException`, 403)
2. 여행 상태가 `COMPLETED`일 것 (`TripNotCompletedException`)
3. 같은 여행에 기존 리뷰가 없을 것 (`AlreadyReviewedException` + DB unique 제약 이중 방어)

작성·수정·삭제 때마다 두 단계의 비정규화 평점을 집계 쿼리로 다시 계산한다.

```
review 변경 → template.avgRating / reviewCount       = 그 템플릿 리뷰의 AVG(실수로) / COUNT
           → guideProfile.totalAvgRating / totalReviewCount = 리뷰 있는 템플릿 평균들의 AVG / 템플릿 리뷰 수의 SUM
```

- 리뷰·템플릿을 엔티티로 불러오지 않으므로, 리뷰가 쌓여도 다시 계산할 때 불러오는 양이 같다.
- 리뷰는 여행에 남지만 노출은 템플릿 상세와 가이드 프로필에서 일어나므로, 리뷰 테이블이 세 축(trip·template·guide_profile)을 모두 직접 참조한다. 리뷰에 보이는 제목은 리뷰한 여행의 제목(사본)이다.

---

## 10. 검색 — QueryDSL 동적 쿼리 (참가의 진입점)

여행자가 여행을 찾는 `GET /api/trips/search`는 `TripRepositoryImpl`의 QueryDSL 쿼리로 처리된다.

| 조건 | 구현 |
| --- | --- |
| 기본 필터 | `status = RECRUITING` 이고 `startDate > 오늘` (모집은 시작 전날까지) |
| 키워드 | 템플릿 제목 OR 소개 OR 가이드 닉네임 `containsIgnoreCase` |
| 날짜 | 구간 겹침: `trip.startDate <= 검색종료 AND trip.endDate >= 검색시작` (한쪽만 있으면 이후 시작/이전 종료로 완화) |
| 지역 | `region.id IN (...) OR region.parent.id IN (...)` — 부모 지역(서울)으로 검색하면 자식 지역(강남구) 여행까지 매칭 |
| 태그 | `template.id IN (서브쿼리: 태그 보유 템플릿)` |
| 정렬 | 시작일 → id. 같은 날짜끼리의 순서를 고정해 페이지 경계에서 빠지거나 겹치지 않게 |
| 페이징 | `limit = size + 1`로 다음 페이지 존재만 판정하는 Slice — COUNT 쿼리 생략 |
| 연관 로딩 | template → guideProfile → user는 fetch join, 목록 카드의 사진·지역(여행 사본)은 배치 로딩으로 여행 수와 무관하게 한 번씩 |

- 검색 조건은 템플릿 기준이다. 모집 중 여행은 템플릿 수정이 막혀 있어 여행 사본과 템플릿의 내용이 같다.
- 조건이 있을 때만 `BooleanBuilder`에 절을 추가하므로, 필터 조합이 늘어도 코드가 조건문 나열로 퇴화하지 않는다.
- 다른 여행 목록(내 여행·참가 여행·찜·가이드 프로필·완료 목록)도 같은 응답 모양(`TripListResponse.from`)을 쓰고, 사진·지역은 배치 로딩, 방 번호는 외래 키 값으로 읽어 여행 수만큼 쿼리가 늘지 않는다.

---

## 11. 알려진 설계 제약

- **단일 서버 전제** — STOMP 브로커가 내장 SimpleBroker이므로 서버를 수평 확장하면 인스턴스 간 브로드캐스트가 끊긴다. 확장 시 외부 브로커(Redis pub/sub, RabbitMQ STOMP 릴레이 등) 도입이 선행되어야 한다.
- **FCM 발송 범위** — 푸시는 SOS에만 붙어 있다. 채팅·공지의 백그라운드 알림은 미구현 상태다.
- **위치 이력 부재** — 최신 스냅샷만 저장하므로 이동 경로 복기 기능은 지원하지 않는다.
- **읽음 처리 컬럼** — `participation.last_chat_read_at`은 스키마에 있으나 갱신 로직이 연결되지 않았다 (안 읽은 메시지 수 기능 미완).

---

## 12. 관련 문서

| 문서 | 내용 |
| --- | --- |
| `TripAgora_ERD.md` | 전체 DB 스키마 (DDL, 제약, enum, 상태 전이) |
| `TripAgora_requirements.md` | 화면별·시스템별 기능 명세 |
| `TripAgora_plan.md` | 서비스 기획서 (문제 정의, 핵심 정책, 기획 대비 구현 범위) |
| `diagrams/TripAgora_flow_diagram.md` | 핵심 흐름 다이어그램 (mermaid) |
