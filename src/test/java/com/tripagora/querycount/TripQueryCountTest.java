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

// 여행 목록(같은 매핑 getTripListResponse를 쓰는 5개 API)과 여행 상세의 쿼리 수 기준선
class TripQueryCountTest extends QueryCountTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
    }

    @ParameterizedTest(name = "{displayName}: 여행 {0}건 → 쿼리 {1}개, 엔티티 {2}개")
    @CsvSource({"5, 16, 37", "10, 31, 72"})
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
    @CsvSource({"5, 22, 37", "10, 42, 72"})
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
    @CsvSource({"5, 22, 46", "10, 42, 91"})
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
    @CsvSource({"5, 22, 41", "10, 42, 81"})
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
    @CsvSource({"5, 25, 37", "10, 45, 72"})
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

    @ParameterizedTest(name = "{displayName}: 참가자 {0}명 → 쿼리 {1}개, 엔티티 {2}개")
    @CsvSource({"2, 11, 15", "4, 13, 19"})
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
