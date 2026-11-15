package com.tripagora.querycount;

import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 여행 목록(같은 매핑 TripListResponse.from을 쓰는 5개 API), 완료 여행 목록, 여행 상세의 쿼리 수 기준선
class TripQueryCountTest extends QueryCountTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
    }

    @ParameterizedTest(name = "{displayName}: 여행 {0}건 → 쿼리 {1}개, 엔티티 {2}개")
    // 사진·지역을 여행 id로 묶어서 불러와 고정 개수가 되고, 여행마다 채팅방 조회 1개만 남는다 (#176 배치 크기. 역방향 1:1이라 배치로 묶이지 않음)
    // 채팅방 외래 키를 여행 쪽으로 옮겨 여행마다 채팅방을 조회하지 않는다 (#176, 쿼리 −여행 수)
    @CsvSource({"5, 3, 37", "10, 3, 72"})
    @DisplayName("여행 검색")
    void searchTrips(int trips, long queries, long entities) throws Exception {
        for (long id = 1; id <= trips; id++) {
            tripWithOwnTemplate(id, GUIDE, TripStatus.RECRUITING);
        }

        Counts counts = measure(() -> mockMvc.perform(get("/api/trips/search").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips", hasSize(trips))));

        assertCounts(counts, queries, entities);
    }

    @ParameterizedTest(name = "{displayName}: 여행 {0}건 → 쿼리 {1}개, 엔티티 {2}개")
    // 가이드 토큰 요청은 인증 필터가 현재 역할을 한 번 확인한다 (#171 PR 2, 쿼리 +1, 엔티티 그대로)
    // 제목·대표 사진·지역을 여행 사본에서 읽어 여행마다 템플릿을 불러오지 않는다 (#174, 쿼리·엔티티 여행 수만큼 감소)
    // 사진·지역을 여행 id로 묶어서 불러와 고정 개수가 되고, 여행마다 채팅방 조회 1개만 남는다 (#176 배치 크기. 역방향 1:1이라 배치로 묶이지 않음)
    // 채팅방 외래 키를 여행 쪽으로 옮겨 여행마다 채팅방을 조회하지 않는다 (#176, 쿼리 −여행 수)
    @CsvSource({"5, 5, 32", "10, 5, 62"})
    @DisplayName("내가 만든 여행 목록")
    void myTrips(int trips, long queries, long entities) throws Exception {
        for (long id = 1; id <= trips; id++) {
            tripWithOwnTemplate(id, GUIDE, TripStatus.RECRUITING);
        }

        Counts counts = measure(() -> mockMvc.perform(get("/api/trips/me").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips", hasSize(trips))));

        assertCounts(counts, queries, entities);
    }

    @ParameterizedTest(name = "{displayName}: 여행 {0}건 → 쿼리 {1}개, 엔티티 {2}개")
    // 제목·대표 사진·지역을 여행 사본에서 읽어 여행마다 템플릿을 불러오지 않는다 (#174, 쿼리·엔티티 여행 수만큼 감소)
    // 여행·사진·지역을 id로 묶어서 불러와 고정 개수 (#176 배치 크기. 여행을 묶어 부르는 SQL에 채팅방이 조인됨)
    // 여행을 읽을 때 채팅방을 함께 불러오지 않는다 (#176 채팅방 외래 키, 엔티티 −여행 수)
    @CsvSource({"5, 5, 36", "10, 5, 71"})
    @DisplayName("참가한 여행 목록 (모집 마감, 채팅방 있음)")
    void participatingTrips(int trips, long queries, long entities) throws Exception {
        for (long id = 1; id <= trips; id++) {
            tripWithOwnTemplate(id, GUIDE, TripStatus.RECRUITMENT_CLOSED);
            fixture.participation(id, id, TRAVELER, Role.TRAVELER);
        }

        Counts counts = measure(() -> mockMvc.perform(get("/api/trips/participating").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips", hasSize(trips))));

        assertCounts(counts, queries, entities);
    }

    @ParameterizedTest(name = "{displayName}: 여행 {0}건 → 쿼리 {1}개, 엔티티 {2}개")
    // 제목·대표 사진·지역을 여행 사본에서 읽어 여행마다 템플릿을 불러오지 않는다 (#174, 쿼리·엔티티 여행 수만큼 감소)
    // 여행·사진·지역을 id로 묶어서 불러와 고정 개수 (#176 배치 크기. 여행을 묶어 부르는 SQL에 채팅방이 조인됨)
    @CsvSource({"5, 5, 36", "10, 5, 71"})
    @DisplayName("찜 목록")
    void wishlist(int trips, long queries, long entities) throws Exception {
        for (long id = 1; id <= trips; id++) {
            tripWithOwnTemplate(id, GUIDE, TripStatus.RECRUITING);
            fixture.wishlist(TRAVELER, id);
        }

        Counts counts = measure(() -> mockMvc.perform(get("/api/wishlist").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips", hasSize(trips))));

        assertCounts(counts, queries, entities);
    }

    @ParameterizedTest(name = "{displayName}: 여행 {0}건 → 쿼리 {1}개, 엔티티 {2}개")
    // 제목·대표 사진·지역을 여행 사본에서 읽어 여행마다 템플릿을 불러오지 않는다 (#174, 쿼리·엔티티 여행 수만큼 감소)
    // 사진·지역을 여행 id로 묶어서 불러와 고정 개수가 되고, 여행마다 채팅방 조회 1개만 남는다 (#176 배치 크기. 역방향 1:1이라 배치로 묶이지 않음)
    // 채팅방 외래 키를 여행 쪽으로 옮겨 여행마다 채팅방을 조회하지 않는다 (#176, 쿼리 −여행 수)
    @CsvSource({"5, 7, 32", "10, 7, 62"})
    @DisplayName("가이드 프로필 (모집중 여행 목록 포함)")
    void guideProfile(int trips, long queries, long entities) throws Exception {
        for (long id = 1; id <= trips; id++) {
            tripWithOwnTemplate(id, GUIDE, TripStatus.RECRUITING);
        }

        Counts counts = measure(() -> mockMvc.perform(get("/api/guide-profiles/" + GUIDE).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips.trips", hasSize(trips))));

        assertCounts(counts, queries, entities);
    }

    @ParameterizedTest(name = "{displayName}: 여행 {0}건 → 쿼리 {1}개, 엔티티 {2}개")
    // 여행·사진·지역을 id로 묶어서 불러와 고정 개수가 되고, 여행마다 '내 리뷰' 조회 1개만 남는다 (#176 배치 크기. 쿼리 메서드 호출이라 배치로 묶이지 않음)
    // 여행을 읽을 때 채팅방을 함께 불러오지 않는다 (#176 채팅방 외래 키, 엔티티 −여행 수)
    // 내 리뷰를 여행 id 목록으로 한 번에 읽는다 (#176, 쿼리 −여행 수 +1)
    @CsvSource({"5, 6, 39", "10, 6, 76"})
    @DisplayName("완료 여행 목록 (홀수 번째 여행만 리뷰 작성)")
    void completedTrips(int trips, long queries, long entities) throws Exception {
        for (long id = 1; id <= trips; id++) {
            tripWithOwnTemplate(id, GUIDE, TripStatus.COMPLETED);
            fixture.participation(id, id, TRAVELER, Role.TRAVELER);
            if (id % 2 == 1) {
                fixture.review(id, TRAVELER, id, id, GUIDE);
            }
        }

        Counts counts = measure(() -> mockMvc.perform(get("/api/trips/completed").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips", hasSize(trips))));

        assertCounts(counts, queries, entities);
    }

    @ParameterizedTest(name = "{displayName}: 참가자 {0}명 → 쿼리 {1}개, 엔티티 {2}개")
    // 참가자의 사용자를 id로 묶어서 불러와 고정 개수 (#176 배치 크기)
    // 여행을 읽을 때 채팅방을 함께 불러오지 않는다 (#176 채팅방 외래 키, 엔티티 −1. 채팅방 id는 외래 키 값으로)
    // 참가 여부를 이미 불러온 참가자 목록으로 확인한다 (#176, 쿼리 −1)
    @CsvSource({"2, 9, 14", "4, 9, 18"})
    @DisplayName("여행 상세 (모집 마감, 가이드 포함 참가자)")
    void tripDetail(int participants, long queries, long entities) throws Exception {
        tripWithOwnTemplate(1L, GUIDE, TripStatus.RECRUITMENT_CLOSED);
        fixture.participation(1L, 1L, GUIDE, Role.GUIDE);
        for (long userId = TRAVELER; userId <= participants; userId++) {
            if (userId != TRAVELER) {
                fixture.traveler(userId);
            }
            fixture.participation(userId, 1L, userId, Role.TRAVELER);
        }

        Counts counts = measure(() -> mockMvc.perform(get("/api/trips/1").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participants", hasSize(participants))));

        assertCounts(counts, queries, entities);
    }
}
