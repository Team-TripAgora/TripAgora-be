package com.tripagora.trip;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 찜, 모집 마감, 여행 삭제
class TripLifecycleTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long CLOSED_TRIP = 1L;
    private static final long RECRUITING_TRIP = 3L;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.template(1L, GUIDE, 2);
        fixture.trip(CLOSED_TRIP, 1L, TripStatus.RECRUITMENT_CLOSED, today.plusDays(30), today.plusDays(31));
        fixture.room(1L, CLOSED_TRIP);
        fixture.trip(RECRUITING_TRIP, 1L, TripStatus.RECRUITING, today.plusDays(60), today.plusDays(61));
    }

    @Test
    @DisplayName("찜을 추가하면 찜 목록에 보이고, 지우면 사라진다")
    void addsAndRemovesWishlist() throws Exception {
        mockMvc.perform(post("/api/wishlist/" + RECRUITING_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/wishlist").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(jsonPath("$.data.trips[*].tripId", contains((int) RECRUITING_TRIP)));

        mockMvc.perform(delete("/api/wishlist/" + RECRUITING_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/wishlist").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(jsonPath("$.data.trips", empty()));
    }

    @Test
    @DisplayName("찜하지 않은 여행을 찜에서 지우면 404")
    void removingMissingWishlistIsNotFound() throws Exception {
        mockMvc.perform(delete("/api/wishlist/" + RECRUITING_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("모집을 마감하면 상태가 바뀌고 채팅방이 생긴다")
    void closingRecruitmentCreatesRoom() throws Exception {
        mockMvc.perform(post("/api/trips/" + RECRUITING_TRIP + "/close").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trips/" + RECRUITING_TRIP).header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(jsonPath("$.data.status").value("RECRUITMENT_CLOSED"))
                .andExpect(jsonPath("$.data.roomId").isNumber());
    }

    @Test
    @DisplayName("모집중인 여행은 삭제된다")
    void deletesRecruitingTrip() throws Exception {
        mockMvc.perform(delete("/api/trips/" + RECRUITING_TRIP).header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trips/" + RECRUITING_TRIP).header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("모집을 마감한 여행은 삭제할 수 없다")
    void cannotDeleteClosedTrip() throws Exception {
        mockMvc.perform(delete("/api/trips/" + CLOSED_TRIP).header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("모집중인 여행만 삭제 가능합니다."));
    }
}
