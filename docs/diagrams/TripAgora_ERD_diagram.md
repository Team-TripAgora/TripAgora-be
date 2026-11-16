# Trip Agora ERD (mermaid)

> 테이블 23개 · MySQL 8. 공통 컬럼 `created_at` / `modified_at`(JPA Auditing)은 지면상 생략.
> 상세 DDL·제약·enum은 [`TripAgora_ERD.md`](../TripAgora_ERD.md) 참조.

```mermaid
erDiagram
    USERS ||--o| GUIDE_PROFILE : "가이드 전환 시"
    USERS ||--o{ REFRESH_TOKEN : ""
    USERS ||--o{ USER_TAG : ""
    USERS ||--o{ PARTICIPATION : ""
    USERS ||--o{ WISHLIST : ""
    USERS ||--o{ REVIEW : "작성"
    USERS ||--o{ CHAT_MESSAGE : "발신"
    USERS ||--o{ USER_LOCATION : ""

    GUIDE_PROFILE ||--o{ GUIDE_PORTFOLIO : ""
    GUIDE_PROFILE ||--o{ TEMPLATE : ""
    GUIDE_PROFILE ||--o{ REVIEW : ""

    TEMPLATE |o--o{ ITINERARY : "템플릿 일정"
    TEMPLATE ||--o{ TEMPLATE_IMAGE : ""
    TEMPLATE ||--o{ TEMPLATE_TAG : ""
    TEMPLATE ||--o{ TEMPLATE_REGION : ""
    TEMPLATE ||--o{ TRIP : "여행 열기"
    TEMPLATE ||--o{ REVIEW : ""

    TAG ||--o{ USER_TAG : ""
    TAG ||--o{ TEMPLATE_TAG : ""
    TAG ||--o{ TRIP_TAG : ""
    REGION ||--o{ TEMPLATE_REGION : ""
    REGION ||--o{ TRIP_REGION : ""
    REGION |o--o{ REGION : "부모-자식"

    TRIP |o--o{ ITINERARY : "여행 일정 (사본)"
    TRIP ||--o{ TRIP_IMAGE : "사본"
    TRIP ||--o{ TRIP_TAG : "사본"
    TRIP ||--o{ TRIP_REGION : "사본"
    TRIP ||--o{ PARTICIPATION : ""
    TRIP ||--o| ROOM : "모집 마감 시 (trip.room_id)"
    TRIP ||--o{ WISHLIST : ""
    TRIP ||--o{ REVIEW : "작성 대상"

    ROOM ||--o{ NOTICE : ""
    ROOM ||--o{ CHAT_MESSAGE : ""
    ROOM ||--o{ USER_LOCATION : ""

    USERS {
        Long id PK
        String social_id "UQ(social_id, social_type)"
        String social_type "KAKAO | NAVER"
        String nickname "UQ"
        String role "현재 활성 역할"
        String image_url "소셜 이미지 S3 재업로드"
        String fcm_token "SOS 푸시용"
    }
    REFRESH_TOKEN {
        Long id PK
        Long user_id FK
        String token "UQ · rotate 저장"
    }
    GUIDE_PROFILE {
        Long id PK
        Long user_id FK "UQ · 1:1"
        String bio
        String image_url
        Double total_avg_rating "집계 컬럼"
        Integer total_review_count "집계 컬럼"
    }
    GUIDE_PORTFOLIO {
        Long id PK
        Long guide_profile_id FK
        String type "SNS·웹사이트 5종"
        String url
    }
    TAG {
        Long id PK "수동 시딩"
        String name "UQ"
    }
    USER_TAG {
        Long user_id PK,FK
        Long tag_id PK,FK
    }
    REGION {
        Long id PK "수동 시딩"
        String name
        int level "계층 깊이"
        Long parent_id FK "self"
    }
    TEMPLATE {
        Long id PK
        Long guide_profile_id FK
        String title "초안은 빈 값"
        String content
        Double avg_rating "집계 컬럼"
        Integer review_count "집계 컬럼"
    }
    TEMPLATE_IMAGE {
        Long id PK
        Long template_id FK
        String image_url "S3"
        int display_order "1 = 대표"
    }
    TEMPLATE_TAG {
        Long template_id PK,FK
        Long tag_id PK,FK
    }
    TEMPLATE_REGION {
        Long template_id PK,FK
        Long region_id PK,FK
    }
    ITINERARY {
        Long id PK
        Long template_id FK "둘 중 하나만 (CHECK)"
        Long trip_id FK "둘 중 하나만 (CHECK)"
        Integer day "1부터 연속"
        String location
        String content
        LocalTime start_time
        Double latitude
        Double longitude
    }
    TRIP {
        Long id PK
        Long template_id FK
        Long room_id FK "UQ · 마감 전엔 NULL"
        String title "열린 시점 사본"
        String content "열린 시점 사본"
        Integer max_participants
        Integer current_participants "행 잠금 후 변경"
        LocalDate start_date
        LocalDate end_date "일정 Day로 계산"
        String status "모집→마감→진행→완료"
    }
    TRIP_IMAGE {
        Long id PK
        Long trip_id FK
        String image_url "템플릿과 같은 S3 파일"
        int display_order "1 = 대표"
    }
    TRIP_TAG {
        Long id PK
        Long trip_id FK "UQ(trip, tag)"
        Long tag_id FK
    }
    TRIP_REGION {
        Long id PK
        Long trip_id FK "UQ(trip, region)"
        Long region_id FK
    }
    PARTICIPATION {
        Long id PK
        Long user_id FK "UQ(user, trip)"
        Long trip_id FK
        String role "여행 안 역할 · 가이드 자동 등록"
        LocalDateTime hidden_at "완료 여행 숨김"
        LocalDateTime last_chat_read_at "갱신 로직 미연결"
    }
    ROOM {
        Long id PK
    }
    NOTICE {
        Long id PK
        Long room_id FK
        String title
        String content "쓰기는 가이드만"
    }
    CHAT_MESSAGE {
        Long id PK
        Long room_id FK
        Long sender_id FK
        String content "1000자"
        Role sender_role "발신 시점 사본"
    }
    USER_LOCATION {
        Long id PK
        Long user_id FK
        Long room_id FK
        Double latitude "(사용자, 방)당 1행 갱신"
        Double longitude
    }
    WISHLIST {
        Long id PK
        Long user_id FK "UQ(user, trip)"
        Long trip_id FK "찜 대상 = 여행"
    }
    REVIEW {
        Long id PK
        Long author_id FK "UQ(author, trip)"
        Long trip_id FK "작성 대상"
        Long template_id FK "상품 상세 조회 축"
        Long guide_profile_id FK "가이드 조회 축"
        String content
        Integer rating "1~5 · 변경 시 평점 재계산"
    }
```

### 읽는 포인트

- **USERS ↔ GUIDE_PROFILE (1:0..1)** — 여행자로 가입, 가이드 전환 시 프로필 생성. 재전환해도 재사용.
- **TEMPLATE → TRIP → ROOM** — 상품 원형(TEMPLATE)에서 여러 날짜의 여행을 열고, 방(ROOM)은 모집 마감 시(가이드의 마감 또는 시작일 00:00 자동 마감)에만 생성. 방 외래 키는 여행 쪽(`trip.room_id`)에 있어 여행 행만 읽어도 방 유무를 안다.
- **여행 = 열린 시점의 사본** — 제목·소개(TRIP), 이미지·태그·지역(TRIP_IMAGE·TRIP_TAG·TRIP_REGION), 일정(ITINERARY의 여행 행)은 여행을 열 때 템플릿에서 복사한다. 템플릿을 고쳐도 열린 여행은 변하지 않는다. 이미지 파일은 복사하지 않고 같은 S3 주소를 쓴다.
- **ITINERARY** 는 템플릿 일정과 여행 일정을 한 테이블에 두고, `chk_itinerary_owner` CHECK로 한 행이 둘 중 정확히 하나에만 속하게 한다.
- **PARTICIPATION.role** 은 여행 안 역할(가이드 자동 등록). 채팅·위치·공지·SOS 인가의 근거. 완료 여행 취소는 행을 지우지 않고 `hidden_at`만 채운다.
- **REVIEW** 는 trip·template·guide_profile 세 축을 모두 직접 참조 — 조회 축(상품/가이드)마다 조인 없이 접근. 평점 집계 컬럼은 리뷰 변경 시 DB 집계 쿼리로 다시 계산.
- **REGION** 은 self-reference로 부모(서울)–자식(강남구) 2단 계층.
