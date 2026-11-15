package com.tripagora.trip;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 여행 응답의 채팅방 id와 참가 여부 (채팅방을 불러오는 방식이나 참가 확인 방식을 바꿔도 같아야 한다)
class TripRoomAndParticipationTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long OUTSIDER = 3L;
    private static final long TEMPLATE = 1L;
    private static final long RECRUITING_TRIP = 1L;
    private static final long CLOSED_TRIP = 2L;
    private static final long COMPLETED_TRIP = 3L;
    // 채팅방 id를 여행 id와 다르게 두어 둘을 헷갈리면 드러나게 한다
    private static final int CLOSED_TRIP_ROOM = 20;
    private static final int COMPLETED_TRIP_ROOM = 30;

    @BeforeEach
    void setUp() {
        LocalDate today = today();
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.traveler(OUTSIDER);
        fixture.template(TEMPLATE, GUIDE, 2);
        fixture.trip(RECRUITING_TRIP, TEMPLATE, TripStatus.RECRUITING, today.plusDays(30), today.plusDays(31));
        fixture.trip(CLOSED_TRIP, TEMPLATE, TripStatus.RECRUITMENT_CLOSED, today.plusDays(10), today.plusDays(11));
        fixture.trip(COMPLETED_TRIP, TEMPLATE, TripStatus.COMPLETED, today.minusDays(10), today.minusDays(9));
        fixture.room(CLOSED_TRIP_ROOM, CLOSED_TRIP);
        fixture.room(COMPLETED_TRIP_ROOM, COMPLETED_TRIP);
        fixture.participation(1L, CLOSED_TRIP, TRAVELER, Role.TRAVELER);
        fixture.participation(2L, COMPLETED_TRIP, TRAVELER, Role.TRAVELER);
        fixture.wishlist(TRAVELER, RECRUITING_TRIP);
    }

    @Test
    @DisplayName("여행 목록에 채팅방 id가 담기고, 채팅방이 없는 모집 중 여행은 null이다")
    void tripListsContainRoomId() throws Exception {
        mockMvc.perform(get("/api/trips/me").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 1)].roomId", contains(nullValue())))
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 2)].roomId", contains(CLOSED_TRIP_ROOM)))
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 3)].roomId", contains(COMPLETED_TRIP_ROOM)));
        mockMvc.perform(get("/api/trips/participating").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 2)].roomId", contains(CLOSED_TRIP_ROOM)))
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 3)].roomId", contains(COMPLETED_TRIP_ROOM)));
        mockMvc.perform(get("/api/trips/search").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 1)].roomId", contains(nullValue())));
        mockMvc.perform(get("/api/wishlist").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 1)].roomId", contains(nullValue())));
        mockMvc.perform(get("/api/trips/completed").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 3)].roomId", contains(COMPLETED_TRIP_ROOM)));
    }

    @Test
    @DisplayName("여행 상세에 채팅방 id가 담기고, 채팅방이 없는 모집 중 여행은 null이다")
    void tripDetailContainsRoomId() throws Exception {
        mockMvc.perform(get("/api/trips/" + CLOSED_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").value(CLOSED_TRIP_ROOM));
        mockMvc.perform(get("/api/trips/" + RECRUITING_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").value(nullValue()));
    }

    @Test
    @DisplayName("여행 상세의 참가 여부는 참가자에게 true, 참가하지 않은 사람에게 false다")
    void tripDetailParticipationFlag() throws Exception {
        mockMvc.perform(get("/api/trips/" + CLOSED_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isParticipating").value(true));
        mockMvc.perform(get("/api/trips/" + CLOSED_TRIP).header("Authorization", bearer(OUTSIDER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isParticipating").value(false));
    }
}
