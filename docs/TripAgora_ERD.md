# Trip Agora DB 스키마 (DDL)

| 항목 | 내용 |
| --- | --- |
| DBMS | MySQL 8 |
| 스키마 관리 | 엔티티 매핑이 스키마의 단일 기준. JPA가 만든다(로컬 `ddl-auto: update`, 테스트 `create-drop`). 별도 마이그레이션 도구 없음 |
| 공통 컬럼 | `BaseEntity`(@MappedSuperclass, JPA Auditing) 상속 테이블은 `created_at`, `modified_at` 자동 관리 (한국 시간) |
| 마스터 데이터 | `region`, `tag`는 `@GeneratedValue` 없는 수동 PK — DB에 직접 INSERT로 시딩 |

> 이 문서의 DDL은 엔티티 매핑으로 JPA가 실제로 만든 스키마(MySQL `SHOW CREATE TABLE`)를 읽기 쉽게 정리한 것이다. 외래 키 이름처럼 JPA가 자동으로 붙인 이름은 생략했다.

---

## 테이블 목록

| # | 테이블 | 도메인 | 설명 | BaseEntity |
| --- | --- | --- | --- | --- |
| 1 | `users` | user | 소셜 로그인 사용자 (여행자/가이드 겸용 단일 계정) | O |
| 2 | `refresh_token` | auth | JWT 리프레시 토큰 저장소 (재발급 시 rotate) | X |
| 3 | `guide_profile` | guideprofile | 가이드 프로필 (1인 1개, 가이드 전환 시 생성) | O |
| 4 | `guide_portfolio` | guideprofile | 가이드 외부 링크 (SNS/웹사이트) | X |
| 5 | `tag` | tag | 관심사 태그 마스터 (수동 시딩) | X |
| 6 | `user_tag` | tag | 사용자–태그 매핑 (복합 PK) | X |
| 7 | `region` | region | 지역 마스터 — 부모/자식 2단 계층 (수동 시딩) | X |
| 8 | `template` | template | 여행 템플릿 (가이드의 재사용 가능한 여행 상품 원형) | O |
| 9 | `template_image` | template | 템플릿 이미지 (S3 URL + 노출 순서) | O |
| 10 | `template_tag` | tag | 템플릿–태그 매핑 (복합 PK) | X |
| 11 | `template_region` | region | 템플릿–지역 매핑 (복합 PK) | X |
| 12 | `itinerary` | itinerary | 일정 항목 — 템플릿 일정과 여행 일정을 한 테이블에 (둘 중 하나에만 속함) | O |
| 13 | `trip` | trip | 여행 — 템플릿으로 연 실제 여행 (날짜·정원·상태 + 열린 시점의 상품 정보 사본) | O |
| 14 | `trip_image` | trip | 여행 이미지 사본 (열린 시점의 템플릿 이미지) | O |
| 15 | `trip_tag` | tag | 여행 태그 사본 | X |
| 16 | `trip_region` | region | 여행 지역 사본 | X |
| 17 | `participation` | participation | 여행 참가 (가이드 포함, 여행 안에서의 역할 보유) | O |
| 18 | `room` | room | 여행당 1개의 소통 방 — 채팅·공지·위치공유의 허브 | O |
| 19 | `notice` | room | 방 공지사항 | O |
| 20 | `chat_message` | chat | 채팅 메시지 (STOMP 수신 시 저장) | O |
| 21 | `user_location` | location | 방 참가자의 최신 위치 (1인 1행) | O |
| 22 | `wishlist` | wishlist | 여행 위시리스트 | O |
| 23 | `review` | review | 여행 리뷰 (작성자당 여행 1건) | O |

---

## DDL

### 1. users

```sql
CREATE TABLE users (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    social_id    VARCHAR(255) NOT NULL,
    social_type  ENUM('KAKAO','NAVER') NOT NULL,
    nickname     VARCHAR(255) NULL,                  -- 최초 로그인 직후엔 미설정, 탈퇴 시 비움
    role         ENUM('ADMIN','GUIDE','TRAVELER') NULL,  -- 현재 활성 역할 (가입 시 TRAVELER)
    image_url    VARCHAR(255) NOT NULL,              -- 소셜 프로필 이미지를 S3로 다시 올린 URL, 탈퇴 시 빈 문자열
    fcm_token    VARCHAR(255) NULL,                  -- FCM 디바이스 토큰 (SOS 푸시 수신용)
    created_at   DATETIME(6)  NOT NULL,
    modified_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_social (social_id, social_type),
    UNIQUE KEY (nickname)
);
```

- 이메일·비밀번호 컬럼이 없다. 인증은 소셜 로그인(카카오/네이버)에 맡기고, 서버는 `(social_id, social_type)` 조합으로만 사용자를 식별한다.
- `role`은 현재 활성 역할이다. 역할 전환 API가 이 값을 바꾸고 JWT를 재발급한다. 가이드 토큰으로 온 요청은 이 값을 확인해, 여행자로 바꾼 뒤의 예전 가이드 토큰을 거부한다.
- **탈퇴는 행을 지우지 않는다.** `social_id`를 `withdrawn-{id}`로 바꿔 다시 로그인되지 않게 하고 닉네임·이미지·FCM 토큰·관심사·찜을 비운다. 다른 사람의 참가·리뷰·채팅 이력이 이 행을 참조하므로 남겨 둔다.

### 2. refresh_token

```sql
CREATE TABLE refresh_token (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    user_id  BIGINT       NOT NULL,
    token    VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY (token),
    FOREIGN KEY (user_id) REFERENCES users (id)
);
```

- 로그인·재발급·역할 전환 때마다 그 사용자의 기존 행을 지우고 새 토큰을 저장한다(rotate). 로그아웃·탈퇴 시에도 지운다. 토큰마다 고유 id(`jti`)가 있어 같은 초에 다시 발급해도 값이 겹치지 않는다.

### 3. guide_profile

```sql
CREATE TABLE guide_profile (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    user_id             BIGINT       NOT NULL,
    bio                 TEXT         NULL,
    image_url           VARCHAR(255) NULL,           -- 가이드 전용 대표 이미지 (users.image_url과 별도)
    total_avg_rating    DOUBLE       NULL,           -- 비정규화: 리뷰 있는 템플릿 평균들의 평균 (초기값 0)
    total_review_count  INT          NULL,           -- 비정규화: 소유 템플릿 리뷰 수 합계 (초기값 0)
    created_at          DATETIME(6)  NOT NULL,
    modified_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES users (id)
);
```

- 최초 가이드 전환 시 1회 생성되며, 이후 여행자로 돌아갔다 다시 전환해도 재사용된다 (1:1).
- 평점 컬럼 갱신 규칙은 아래 "비정규화 평점 컬럼과 갱신 규칙" 참조. 탈퇴 시 소개·이미지·외부 링크를 비운다.

### 4. guide_portfolio

```sql
CREATE TABLE guide_portfolio (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    guide_profile_id  BIGINT       NOT NULL,
    type              ENUM('FACEBOOK','INSTAGRAM','TWITTER','WEBSITE','YOUTUBE') NOT NULL,
    url               VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (guide_profile_id) REFERENCES guide_profile (id)
);
```

- 수정은 전체 교체 방식(최대 5개): `PUT /api/guide-profiles/me/portfolios`가 기존 목록을 비우고 요청 목록으로 다시 채운다 (`orphanRemoval`).

### 5. tag / 6. user_tag

```sql
CREATE TABLE tag (
    id    BIGINT       NOT NULL,               -- 수동 PK (마스터 데이터)
    name  VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY (name)
);

CREATE TABLE user_tag (
    user_id  BIGINT NOT NULL,
    tag_id   BIGINT NOT NULL,
    PRIMARY KEY (tag_id, user_id),             -- @EmbeddedId 복합 PK
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (tag_id)  REFERENCES tag (id)
);
```

- 사용자 관심사는 최소 3개 필수, 갱신은 전체 교체.

### 7. region

```sql
CREATE TABLE region (
    id         BIGINT       NOT NULL,          -- 수동 PK (마스터 데이터)
    name       VARCHAR(255) NOT NULL,
    level      INT          NOT NULL,          -- 계층 깊이 (상위 지역 / 하위 지역)
    parent_id  BIGINT       NULL,              -- self-reference
    PRIMARY KEY (id),
    FOREIGN KEY (parent_id) REFERENCES region (id)
);
```

- 부모(예: 서울)–자식(예: 강남구) 2단 계층. 검색 시 부모 ID로 조회하면 자식 지역의 여행까지 함께 걸리도록 QueryDSL에서 parent 조인을 사용한다.
- 템플릿에 부모와 자식을 동시에 붙이는 것은 금지된다 (`RegionHierarchyConflictException`).

### 8. template

```sql
CREATE TABLE template (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    guide_profile_id  BIGINT       NOT NULL,
    title             VARCHAR(255) NOT NULL,   -- 초안 생성 시 빈 문자열로 시작
    content           VARCHAR(255) NOT NULL,   -- 초안 생성 시 빈 문자열로 시작
    avg_rating        DOUBLE       NULL,       -- 비정규화: 이 템플릿 리뷰 평균 (초기값 0)
    review_count      INT          NULL,       -- 비정규화: 이 템플릿 리뷰 수 (초기값 0)
    created_at        DATETIME(6)  NOT NULL,
    modified_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (guide_profile_id) REFERENCES guide_profile (id)
);
```

- 상태 컬럼(DRAFT/PUBLISHED 등)이 없다. `POST /api/templates`가 빈 초안 행을 만들고, 제목·내용·이미지·태그·지역·일정을 각각의 PATCH/PUT API로 채우는 단계식 작성 구조다.
- 모집·마감·진행 중 여행(RECRUITING·RECRUITMENT_CLOSED·IN_PROGRESS)이 있으면 수정이 막힌다. 여행을 한 번이라도 연 템플릿은 삭제할 수 없다(그 여행의 참가·리뷰·채팅 이력을 지키기 위해) (`TemplateHasActiveTripsException`).

### 9. template_image

```sql
CREATE TABLE template_image (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    template_id    BIGINT       NOT NULL,
    image_url      VARCHAR(255) NOT NULL,      -- S3 객체 URL
    display_order  INT          NULL,          -- 1부터 시작, 1번이 대표 이미지
    created_at     DATETIME(6)  NOT NULL,
    modified_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (template_id) REFERENCES template (id)
);
```

- 이미지 갱신은 전체 교체: 새 파일을 올린 뒤 행을 바꾸고, 옛 S3 파일은 DB 커밋 뒤에 지운다. 단, `trip_image`가 아직 쓰는 파일은 지우지 않는다(템플릿과 여행 사본이 같은 파일을 함께 쓴다).

### 10. template_tag / 11. template_region

```sql
CREATE TABLE template_tag (
    template_id  BIGINT NOT NULL,
    tag_id       BIGINT NOT NULL,
    PRIMARY KEY (tag_id, template_id),
    FOREIGN KEY (template_id) REFERENCES template (id),
    FOREIGN KEY (tag_id)      REFERENCES tag (id)
);

CREATE TABLE template_region (
    template_id  BIGINT NOT NULL,
    region_id    BIGINT NOT NULL,
    PRIMARY KEY (region_id, template_id),
    FOREIGN KEY (template_id) REFERENCES template (id),
    FOREIGN KEY (region_id)   REFERENCES region (id)
);
```

- 템플릿 태그는 최대 5개(요청 검증). 검색의 태그·지역 필터는 이 두 테이블을 쓴다.

### 12. itinerary

```sql
CREATE TABLE itinerary (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    template_id  BIGINT       NULL,            -- 템플릿 일정이면 채움
    trip_id      BIGINT       NULL,            -- 여행 일정이면 채움
    day          INT          NOT NULL,        -- 1일차, 2일차, …
    location     VARCHAR(255) NOT NULL,        -- 장소명
    content      TEXT         NULL,            -- 일정 설명
    start_time   TIME(6)      NOT NULL,        -- 그 일정의 시작 시각
    latitude     DOUBLE       NOT NULL,
    longitude    DOUBLE       NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    modified_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (template_id) REFERENCES template (id),
    FOREIGN KEY (trip_id)     REFERENCES trip (id),
    CONSTRAINT chk_itinerary_owner CHECK ((template_id IS NULL) <> (trip_id IS NULL))  -- 정확히 하나에만 속함
);
```

- 템플릿 일정과 여행 일정은 구조가 같아 한 테이블에 둔다. `chk_itinerary_owner`가 한 행이 템플릿과 여행 중 **정확히 하나**에만 속하게 한다.
- 조회 시 정렬은 엔티티의 `@OrderBy("day ASC, startTime ASC")`가 맡는다.
- 일정 갱신은 전체 교체 방식이며, Day 번호는 1부터 빈 번호 없이 연속해야 한다 (`ItineraryDaySequenceInvalidException`).
- 여행 일정은 여행을 열 때 템플릿 일정을 값으로 복사한 것이다. 이후 템플릿을 고쳐도 여행 일정은 변하지 않고, 반대로 진행 중 여행의 일정 수정도 템플릿 원본에 영향을 주지 않는다.

### 13. trip

```sql
CREATE TABLE trip (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    template_id           BIGINT       NOT NULL,
    room_id               BIGINT       NULL,       -- 모집 마감 때 만든 방 (그 전에는 NULL)
    title                 VARCHAR(255) NOT NULL,   -- 열린 시점의 템플릿 제목 사본
    content               VARCHAR(255) NOT NULL,   -- 열린 시점의 템플릿 소개 사본
    max_participants      INT          NOT NULL,
    current_participants  INT          NOT NULL,   -- 초기값 0
    start_date            DATE         NOT NULL,
    end_date              DATE         NOT NULL,   -- startDate + (일정 최대 Day - 1)로 서버가 계산
    status                ENUM('COMPLETED','IN_PROGRESS','RECRUITING','RECRUITMENT_CLOSED') NOT NULL,
    created_at            DATETIME(6)  NOT NULL,
    modified_at           DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY (room_id),
    FOREIGN KEY (template_id) REFERENCES template (id),
    FOREIGN KEY (room_id)     REFERENCES room (id)
);
```

- 종료일은 클라이언트 입력이 아니라 템플릿 일정의 최대 Day에서 계산된다. 일정이 없으면 당일 여행으로 간주.
- **상품 정보 사본**: 제목·소개(이 테이블)와 이미지·태그·지역(`trip_image`·`trip_tag`·`trip_region`)은 여행을 열 때 템플릿에서 복사한다. 참가자가 보고 신청한 그 시점의 기록이라, 템플릿을 고쳐도 지난 여행의 화면과 리뷰 제목은 그대로다. 가이드 정보(닉네임·사진·평점)는 사본을 두지 않고 최신을 참조한다.
- **방 외래 키는 여행 쪽에 있다**(`room_id`). 여행 행만 읽어도 방 유무와 id를 알 수 있어, 여행 목록을 읽을 때 방을 따로 조회하지 않는다.
- 인원(`current_participants`)을 바꾸는 작업(참가 신청·취소)과 정원 수정·모집 마감·삭제는 이 행을 잠그고(`SELECT … FOR UPDATE`) 시작한다. 동시에 들어온 신청이 정원을 넘기지 않는다.
- 가격 컬럼이 없다 — 결제·정산은 서비스 범위에서 제외된 상태다 (`TripAgora_plan.md`의 "기획 대비 구현 범위" 참조).

### 14. trip_image / 15. trip_tag / 16. trip_region

```sql
CREATE TABLE trip_image (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    trip_id        BIGINT       NOT NULL,
    image_url      VARCHAR(255) NOT NULL,      -- 템플릿 이미지와 같은 S3 파일 주소
    display_order  INT          NULL,
    created_at     DATETIME(6)  NOT NULL,
    modified_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (trip_id) REFERENCES trip (id)
);

CREATE TABLE trip_tag (
    id       BIGINT NOT NULL AUTO_INCREMENT,
    trip_id  BIGINT NOT NULL,
    tag_id   BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trip_tag (trip_id, tag_id),
    FOREIGN KEY (trip_id) REFERENCES trip (id),
    FOREIGN KEY (tag_id)  REFERENCES tag (id)
);

CREATE TABLE trip_region (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    trip_id    BIGINT NOT NULL,
    region_id  BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trip_region (trip_id, region_id),
    FOREIGN KEY (trip_id)   REFERENCES trip (id),
    FOREIGN KEY (region_id) REFERENCES region (id)
);
```

- 여행을 열 때 템플릿의 이미지·태그·지역을 복사한 사본이다. 여행 상세·목록·완료 목록은 이 사본을 읽는다.
- 템플릿 쪽 매핑 테이블과 달리 복합 키가 아니라 대리 키 + unique 제약이다. 여행은 저장될 때 id가 정해지는데 사본은 여행을 만들면서 함께 만들기 때문이다.
- 이미지 파일은 복사하지 않고 같은 주소를 쓴다. 템플릿 이미지를 바꿀 때 이 테이블이 쓰는 파일은 지우지 않는다.

### 17. participation

```sql
CREATE TABLE participation (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            BIGINT       NULL,
    trip_id            BIGINT       NULL,
    role               ENUM('ADMIN','GUIDE','TRAVELER') NOT NULL,  -- 이 여행 안에서의 역할: GUIDE | TRAVELER
    hidden_at          DATETIME(6)  NULL,       -- 완료 여행을 내 목록에서 숨긴 시각
    last_chat_read_at  DATETIME(6)  NULL,       -- 채팅 읽음 처리용 컬럼 (갱신 로직 미연결)
    created_at         DATETIME(6)  NOT NULL,
    modified_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_participation_user_trip (user_id, trip_id),
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (trip_id) REFERENCES trip (id)
);
```

- 여행을 열 때 가이드 본인의 행이 `role = GUIDE`로 자동 생성된다. 이 행이 채팅·위치공유·공지·SOS 접근 권한의 근거가 되므로, 가이드와 여행자가 같은 인가 경로(참가 여부 확인)를 탄다.
- `role`은 `users.role`(계정의 현재 활성 역할)과 다르다. 계정 역할은 전환될 수 있지만, 여행 안에서의 역할은 유지된다.
- `uk_participation_user_trip`은 같은 사람이 같은 여행에 두 번 참가하지 못하게 하는 마지막 방어선이다(신청은 사용자 행 → 여행 행을 잠그고 확인한다).
- 참가 취소는 모집 중에만 행을 지운다. 완료된 여행의 취소는 행을 남기고 `hidden_at`만 채워 내 목록에서 뺀다(다른 참가자가 보는 이력과 인원은 그대로).

### 18. room / 19. notice

```sql
CREATE TABLE room (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    created_at   DATETIME(6) NOT NULL,
    modified_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE notice (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    room_id      BIGINT       NULL,
    title        VARCHAR(255) NOT NULL,
    content      VARCHAR(255) NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    modified_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (room_id) REFERENCES room (id)
);
```

- 방은 여행과 1:1이지만 여행을 열 때가 아니라 **모집 마감 시점**에 만들어진다(가이드의 마감 또는 시작일 자동 마감). 모집 중에는 방이 없고(`trip.room_id = NULL`), 마감과 동시에 채팅·공지·위치공유가 열린다. 여행이 `room_id`로 방을 가리킨다.
- 방(채팅·위치·공지)에는 그 여행의 참가자만 들어올 수 있다. 공지 작성·수정·삭제는 그 여행에서 `role = GUIDE`인 참가자만 가능하다.

### 20. chat_message

```sql
CREATE TABLE chat_message (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    room_id      BIGINT        NOT NULL,
    sender_id    BIGINT        NOT NULL,
    content      VARCHAR(1000) NOT NULL,   -- 1~1000자 (요청 검증)
    sender_role  TINYINT       NOT NULL,   -- Role의 순번 (0=TRAVELER, 1=GUIDE, 2=ADMIN). @Enumerated 미지정으로 JPA 기본값(ORDINAL)
    created_at   DATETIME(6)   NOT NULL,
    modified_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (room_id)   REFERENCES room (id),
    FOREIGN KEY (sender_id) REFERENCES users (id),
    CHECK (sender_role BETWEEN 0 AND 2)
);
```

- `sender_role`은 발신 시점의 여행 안 역할 사본이다. 이후 발신자가 역할을 전환해도 과거 메시지의 가이드/여행자 구분(말풍선 UI)이 유지된다.
- 메시지는 STOMP 수신 시점에 저장되고, 과거 내역은 REST(`GET /api/rooms/{roomId}/messages`)로 Slice 페이징 조회한다.

### 21. user_location

```sql
CREATE TABLE user_location (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    room_id      BIGINT      NOT NULL,
    latitude     DOUBLE      NOT NULL,     -- −90 ~ 90 (요청 검증)
    longitude    DOUBLE      NOT NULL,     -- −180 ~ 180 (요청 검증)
    created_at   DATETIME(6) NOT NULL,
    modified_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (room_id) REFERENCES room (id)
);
```

- 위치 이력 테이블이 아니라 **최신 위치 스냅샷** 테이블이다. (사용자, 방)당 1행을 저장·갱신하며, `modified_at`이 마지막 갱신 시각 역할을 한다. 위치는 STOMP 브로드캐스트로 실시간 전파되므로 DB에는 최신값만 남긴다 — 방에 들어온 직후 전원의 현재 위치를 그리기 위한 용도. 탈퇴 시 지운다.

### 22. wishlist

```sql
CREATE TABLE wishlist (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    trip_id      BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    modified_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wishlist_user_trip (user_id, trip_id),
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (trip_id) REFERENCES trip (id)
);
```

- 찜 대상은 템플릿이 아니라 **여행**이다. 여행자가 탐색하는 단위(날짜가 정해진 실제 모집 건)를 그대로 저장한다.
- 중복 추가는 애플리케이션 검사(`AlreadyInWishlistException`)와 `uk_wishlist_user_trip` unique 제약으로 막는다(동시 요청 포함).

### 23. review

```sql
CREATE TABLE review (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    author_id         BIGINT      NOT NULL,
    trip_id           BIGINT      NOT NULL,
    template_id       BIGINT      NOT NULL,
    guide_profile_id  BIGINT      NOT NULL,
    content           TEXT        NOT NULL,
    rating            INT         NOT NULL,   -- 1~5 (요청 검증)
    created_at        DATETIME(6) NOT NULL,
    modified_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_author_trip (author_id, trip_id),
    FOREIGN KEY (author_id)        REFERENCES users (id),
    FOREIGN KEY (trip_id)          REFERENCES trip (id),
    FOREIGN KEY (template_id)      REFERENCES template (id),
    FOREIGN KEY (guide_profile_id) REFERENCES guide_profile (id)
);
```

- 여행·템플릿·가이드 프로필 세 FK를 모두 갖는다. 리뷰는 "이 여행"에 남기지만, 조회는 템플릿 단위(상품 상세)와 가이드 단위(가이드 프로필) 양쪽에서 일어나기 때문에 조인 없이 바로 걸 수 있도록 참조를 함께 보관한다. 리뷰에 보이는 제목은 리뷰한 여행의 제목이다.
- 작성 조건: 그 여행의 `TRAVELER` 참가자 + 여행 상태 `COMPLETED` + 미작성 건. 중복 방지는 애플리케이션 검사와 `uk_review_author_trip` unique 제약의 이중 방어.

---

## 비정규화 평점 컬럼과 갱신 규칙

`template.avg_rating / review_count`, `guide_profile.total_avg_rating / total_review_count`는 조회 성능을 위한 비정규화 컬럼이다. 목록·상세 화면마다 리뷰 집계를 반복하지 않도록, **쓰기 시점(리뷰 생성·수정·삭제)에 다시 계산**한다.

```
리뷰 생성/수정/삭제
  → 그 템플릿의 리뷰를 집계 쿼리로 다시 계산 → template.avg_rating(평균), review_count(개수) 갱신
  → 그 가이드의 템플릿을 집계 쿼리로 다시 계산 → guide_profile.total_avg_rating(리뷰 있는 템플릿 평균들의 평균),
                                                  total_review_count(템플릿 리뷰 수 합계) 갱신
```

- 리뷰·템플릿을 엔티티로 불러오지 않고 DB의 `COUNT`·`AVG`로 계산한다. 정수 평점의 평균은 실수로 바꿔 계산해 소수점이 잘리지 않는다.
- 가이드 평점은 "모든 리뷰의 평균"이 아니라 "리뷰가 1건 이상 있는 템플릿별 평균의 평균"이다. 리뷰가 특정 인기 템플릿에 몰려도 다른 템플릿의 평가가 희석되지 않도록 한 계산 방식이다.

---

## 테이블 관계 요약

| 관계 | 카디널리티 | 설명 |
| --- | --- | --- |
| users → refresh_token | 1 : N | 로그인 토큰 (rotate로 실질 1행 유지) |
| users → guide_profile | 1 : 0..1 | 가이드 전환 시 생성, 재전환 시 재사용 |
| guide_profile → guide_portfolio | 1 : N | 외부 링크 목록 (전체 교체 갱신) |
| users ↔ tag | N : M (`user_tag`) | 관심사 (최소 3개) |
| region → region | 1 : N (self) | 부모 지역–자식 지역 계층 |
| guide_profile → template | 1 : N | 가이드의 여행 상품 원형 |
| template → itinerary | 1 : N | 템플릿 일정 (Day + 시각 정렬) |
| template → template_image | 1 : N | 이미지 (display_order 1번이 대표) |
| template ↔ tag / region | N : M | 검색 필터의 근거 |
| template → trip | 1 : N | 하나의 원형에서 여러 날짜의 여행을 연다 |
| trip → itinerary | 1 : N | 열린 시점의 템플릿 일정 사본 (여행 중 수정 가능) |
| trip → trip_image / trip_tag / trip_region | 1 : N | 열린 시점의 상품 정보 사본 |
| trip ↔ users | N : M (`participation`) | 참가 (가이드 포함, 여행 안 역할 보유) |
| trip → room | 0..1 : 1 (`trip.room_id`) | 모집 마감 시 생성 |
| room → notice / chat_message / user_location | 1 : N | 방 단위 소통 데이터 |
| trip → wishlist | 1 : N | 여행 단위 찜 |
| trip/template/guide_profile → review | 1 : N | 삼중 참조 (조회 축별 직접 조인) |

- 삭제 전파는 JPA `cascade = ALL, orphanRemoval = true`로 관리된다. 여행 삭제(모집 중에만)는 그 여행의 참가·일정·사본·찜과 함께 지워진다. 템플릿은 여행을 연 적이 없을 때만 삭제되며 그 일정·이미지·태그·지역과 함께 지워진다.
- 회원 탈퇴는 삭제가 아니라 익명화다(`users` 참조).

---

## 주요 상태값 (Enum)

| Enum | 값 | 저장 방식 | 사용처 |
| --- | --- | --- | --- |
| `SocialType` | `KAKAO`, `NAVER` | STRING | users.social_type |
| `Role` | `TRAVELER`, `GUIDE`, `ADMIN`* | STRING (chat_message.sender_role만 ORDINAL) | users.role, participation.role, chat_message.sender_role |
| `TripStatus` | `RECRUITING`, `RECRUITMENT_CLOSED`, `IN_PROGRESS`, `COMPLETED` | STRING | trip.status |
| `GuidePortfolio.PortfolioType` | `FACEBOOK`, `INSTAGRAM`, `TWITTER`, `WEBSITE`, `YOUTUBE` | STRING | guide_portfolio.type |

\* `ADMIN`은 enum에 정의만 되어 있고 발급·검사 경로가 없다 (관리자 기능 미구현).

### TripStatus 전이 흐름

```
                (가이드) POST /api/trips/{tripId}/close        (스케줄러) 00:00 시작일 자동 마감
                 · 시작일 전 → RECRUITMENT_CLOSED                · 시작일이 오늘 이하 → IN_PROGRESS
                 · 시작일 당일·이후 → IN_PROGRESS (바로 진입)
RECRUITING ──────────────┬────────────────────────────┐
                         ▼                            ▼
               RECRUITMENT_CLOSED ──────────▶   IN_PROGRESS ──────────▶ COMPLETED
                         스케줄러 00:01                  스케줄러 00:03
                         (시작일이 오늘 이하)            (종료일이 지난 여행)
```

- 상태를 되돌리는 전이는 없다 (취소 상태 없음 — 모집 중 여행은 삭제로만 회수 가능).
- 모집은 시작 전날까지다. 가이드가 마감하지 않은 여행도 시작일 00:00에 자동으로 마감되어 진행 중이 된다(방 생성 포함). 스케줄러는 한국 시간 기준이며 "이하"로 잡아 하루를 건너뛰어도 따라잡는다.
- 상태별 허용 액션: 참가 신청은 `RECRUITING`(시작 전날까지), 여행 수정·삭제는 `RECRUITING`, 일정 수정·SOS는 `IN_PROGRESS`, 리뷰 작성은 `COMPLETED`에서만.

---

## 관련 문서

| 문서 | 내용 |
| --- | --- |
| `TripAgora_plan.md` | 서비스 기획서 (문제 정의, 핵심 정책, 아키텍처 의도) |
| `TripAgora_requirements.md` | 화면별·시스템별 기능 명세 |
| `TripAgora_trip_realtime_flow.md` | 여행 라이프사이클 × 실시간 통신 흐름 상세 |
| `diagrams/TripAgora_ERD_diagram.md` | ERD (mermaid) |
