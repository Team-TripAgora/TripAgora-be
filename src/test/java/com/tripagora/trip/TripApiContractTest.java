package com.tripagora.trip;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 여행 API의 경로와 응답 필드 이름 (#144에서 바꾼 계약)
class TripApiContractTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long ROOM = 1L;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.template(1L, GUIDE, 2);
        fixture.trip(1L, 1L, TripStatus.RECRUITMENT_CLOSED, today.plusDays(30), today.plusDays(31));
        fixture.room(ROOM, 1L);
        fixture.participation(1L, 1L, GUIDE, Role.GUIDE);
        fixture.participation(2L, 1L, TRAVELER, Role.TRAVELER);
        fixture.trip(3L, 1L, TripStatus.RECRUITING, today.plusDays(60), today.plusDays(61));
    }

    @Test
    @DisplayName("내가 만든 여행 목록은 /api/trips/me 이고 trips·tripId 로 준다")
    void myTripsUsesTripsAndTripId() throws Exception {
        mockMvc.perform(get("/api/trips/me").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips[0].tripId").isNumber())
                .andExpect(jsonPath("$.data.sessions").doesNotExist());
    }

    @Test
    @DisplayName("여행 상세는 isMyTrip 으로 준다")
    void tripDetailUsesIsMyTrip() throws Exception {
        mockMvc.perform(get("/api/trips/1").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isMyTrip").value(true))
                .andExpect(jsonPath("$.data.isMySession").doesNotExist());
    }

    @Test
    @DisplayName("여행 개설 응답은 tripId 로 준다")
    void createTripReturnsTripId() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":1,\"maxParticipants\":4,\"startDate\":\"" + today.plusDays(90) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tripId").isNumber());
    }

    @Test
    @DisplayName("참가 신청은 /api/participation/trips/{tripId} 이고 tripId 로 준다")
    void participationUsesTripPath() throws Exception {
        mockMvc.perform(post("/api/participation/trips/3").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tripId").value(3));
    }

    @Test
    @DisplayName("내 가이드 프로필은 /api/guide-profiles/me 이고 여행 목록을 trips 로 준다")
    void myGuideProfileUsesTrips() throws Exception {
        mockMvc.perform(get("/api/guide-profiles/me").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips").exists())
                .andExpect(jsonPath("$.data.SessionList").doesNotExist());
    }

    @Test
    @DisplayName("내 템플릿 목록은 /api/templates/me 이다")
    void myTemplatesUsesMe() throws Exception {
        mockMvc.perform(get("/api/templates/me").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("채팅 메시지와 위치 조회는 /api/rooms/{roomId} 아래에 있다")
    void roomSubResourcesUseRoomsPath() throws Exception {
        mockMvc.perform(get("/api/rooms/" + ROOM + "/messages").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/rooms/" + ROOM + "/locations").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("옛 여행 경로는 없다")
    void oldSessionPathsAreGone() throws Exception {
        mockMvc.perform(get("/api/sessions/1").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/chat/room/" + ROOM + "/messages").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isNotFound());
    }
}
