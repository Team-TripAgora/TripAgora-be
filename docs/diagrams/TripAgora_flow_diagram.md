# Trip Agora 핵심 플로우 (mermaid)

> 여행 라이프사이클 × 실시간 통신 파이프라인. 단계별 상세는 [`TripAgora_trip_realtime_flow.md`](../TripAgora_trip_realtime_flow.md) 참조.

## 1. 전체 파이프라인

```mermaid
flowchart TD
    subgraph GUIDE["가이드"]
        T1["① 템플릿 초안 생성<br/>POST /api/templates"]
        T2["제목·소개·이미지·태그·지역<br/>PATCH 단계별 저장"]
        T3["일정 등록 Day별<br/>PUT /itineraries"]
        S1["② 여행 열기<br/>POST /api/trips<br/>일정·상품 정보 사본 복사 · 종료일 자동 계산<br/>가이드 Participation 자동 생성"]
        C1["④ 모집 마감<br/>POST /trips/{tripId}/close<br/>시작일 전 → 마감 · 당일 이후 → 진행 중"]
    end

    subgraph TRAVELER["여행자"]
        E1["③ 탐색<br/>GET /trips/search (QueryDSL)<br/>시작일이 지나지 않은 모집 중 여행"]
        P1["참가 신청<br/>POST /participation/trips/{tripId}"]
        L1["사용자 → 여행 행 잠금<br/>SELECT … FOR UPDATE"]
        P2{"잠근 뒤 검증<br/>모집 중·시작 전날까지 · 본인 여행<br/>중복 · 기간 겹침 · 정원"}
    end

    T1 --> T2 --> T3 --> S1
    S1 --> E1
    E1 --> P1 --> L1 --> P2
    P2 -->|통과| P3["참가 확정<br/>currentParticipants++"]
    P2 -->|실패| PX["예외 반환"]
    S1 --> C1
    P3 --> C1
    P3 --> AC["④ 시작일 00:00 자동 마감<br/>TripScheduler (KST)<br/>마감하지 않은 여행 → 진행 중"]
    C1 --> R1["방 생성 · trip.room_id<br/>여행 행 잠근 뒤 상태 재확인 → 방은 하나"]
    AC --> R1
    R1 --> RT["⑤ 실시간 구간<br/>채팅 · 위치 공유 · 공지 · SOS"]

    RT --> SCH["⑥ 자동 상태 전환<br/>TripScheduler (KST)<br/>00:01 진행 · 00:03 완료"]
    SCH --> RV["⑦ 리뷰<br/>COMPLETED + TRAVELER + 미작성<br/>평점 집계 쿼리로 재계산"]

    classDef guide fill:#EAF2FE,stroke:#3182F6,color:#191F28;
    classDef trav fill:#F3EEFB,stroke:#7C3AED,color:#191F28;
    classDef sys fill:#FFF3E0,stroke:#E8833A,color:#191F28;
    class T1,T2,T3,S1,C1 guide;
    class E1,P1,L1,P2,P3 trav;
    class AC,R1,RT,SCH,RV sys;
```

## 2. 실시간 구간 — STOMP 프레임별 인가

```mermaid
sequenceDiagram
    participant App as 앱 (참가자)
    participant WS as /connect (SockJS)
    participant IC as StompAuthInterceptor
    participant Ctrl as StompController
    participant RA as RoomAccessValidator
    participant DB as MySQL

    App->>WS: STOMP CONNECT (Authorization: Bearer)
    WS->>IC: preSend(CONNECT)
    IC->>IC: ① JWT 검증 → userId·role Principal
    IC-->>App: CONNECTED (하트비트 10초)

    App->>IC: SUBSCRIBE /topic/room/{roomId}/chat
    IC->>IC: ② 주소 형식 확인 (RoomDestinations)
    IC->>RA: 방 번호 + 나로 참가 확인
    RA->>DB: 참가 조회 1번 (trip.room_id로 JOIN)
    alt 참가자 아님 / 방 없음
        IC-->>App: ERROR 프레임 (권한 없음 / 방 없음)
    else 참가자
        IC-->>App: 구독 성공
    end

    App->>IC: SEND /publish/room/{roomId}/chat
    IC->>IC: ③ 인증 + 주소 형식 확인
    IC->>Ctrl: @MessageMapping
    Ctrl->>RA: 참가 확인
    Ctrl->>DB: 메시지 저장 (senderRole 사본)
    Ctrl-->>App: /topic/room/{roomId}/chat 브로드캐스트 (참가자 전원)
    opt 처리 중 오류 (입력 검증·권한 등)
        Ctrl-->>App: /user/queue/errors (보낸 사람에게만)
    end
```

> 위치 공유도 같은 경로다(`/publish/room/{roomId}/location` → 최신 위치 저장·갱신 → `/topic/room/{roomId}/location`). 내 오류 큐 `/user/queue/errors` 구독은 로그인만 확인한다.

## 3. SOS는 STOMP가 아닌 FCM

```mermaid
flowchart LR
    U["참가자 SOS 요청<br/>POST /rooms/{roomId}/sos"] --> V{"참가자인가 (403)<br/>진행 중 여행인가 (400)"}
    V -->|통과| F["받는 사람 FCM 토큰 조회<br/>같은 여행 · 보낸 사람 제외<br/>(쿼리 1번)"]
    F --> FCM["Firebase FCM<br/>sendEachForMulticast<br/>트랜잭션 밖에서 발송"]
    FCM --> D["앱 백그라운드<br/>OS 푸시 수신<br/>type=SOS, tripId"]

    classDef n fill:#FFF3E0,stroke:#E8833A,color:#191F28;
    class U,V,F,FCM,D n;
```

> STOMP 브로드캐스트는 소켓이 연결된(화면을 보고 있는) 참가자에게만 닿는다. 긴급 신호는 앱을 보고 있지 않은 사람에게도 도달해야 하므로 OS 레벨 푸시(FCM)를 사용한다. 외부 호출 동안 DB 커넥션을 잡지 않도록 조회를 마친 뒤 발송하고, 발송 실패는 로그만 남긴다.
