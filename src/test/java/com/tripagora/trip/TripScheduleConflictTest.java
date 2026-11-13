package com.tripagora.trip;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 가이드는 아직 끝나지 않은 자기 여행(모집중·모집마감·진행중)과 날짜가 겹치는 여행을 열거나 옮길 수 없다
class TripScheduleConflictTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TEMPLATE = 1L;
    private static final String CONFLICT_MESSAGE = "이미 해당 날짜에 진행 중인 여행이 존재합니다.";

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.template(TEMPLATE, GUIDE, 2);
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITMENT_CLOSED, today.plusDays(30), today.plusDays(31));
        fixture.trip(2L, TEMPLATE, TripStatus.IN_PROGRESS, today.minusDays(1), today);
        fixture.trip(3L, TEMPLATE, TripStatus.RECRUITING, today.plusDays(60), today.plusDays(61));
    }

    @Test
    @DisplayName("모집마감 여행과 날짜가 겹치면 개설할 수 없다")
    void rejectsCreateOverlappingClosedTrip() throws Exception {
        createTrip(today.plusDays(31))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(CONFLICT_MESSAGE));
    }

    @Test
    @DisplayName("진행중 여행과 날짜가 겹치면 개설할 수 없다")
    void rejectsCreateOverlappingInProgressTrip() throws Exception {
        createTrip(today)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(CONFLICT_MESSAGE));
    }

    @Test
    @DisplayName("모집중 여행과 날짜가 겹치면 개설할 수 없다")
    void rejectsCreateOverlappingRecruitingTrip() throws Exception {
        createTrip(today.plusDays(61))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(CONFLICT_MESSAGE));
    }

    @Test
    @DisplayName("겹치는 여행이 없으면 개설된다")
    void createsTripWithoutOverlap() throws Exception {
        createTrip(today.plusDays(90))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tripId").isNumber());
    }

    @Test
    @DisplayName("모집중 여행의 시작일을 모집마감 여행과 겹치게 옮길 수 없다")
    void rejectsUpdateOverlappingClosedTrip() throws Exception {
        mockMvc.perform(patch("/api/trips/3")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxParticipants\":4,\"startDate\":\"" + today.plusDays(29) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(CONFLICT_MESSAGE));
    }

    private ResultActions createTrip(LocalDate startDate) throws Exception {
        return mockMvc.perform(post("/api/trips")
                .header("Authorization", bearer(GUIDE, Role.GUIDE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"templateId\":" + TEMPLATE + ",\"maxParticipants\":4,\"startDate\":\"" + startDate + "\"}"));
    }
}
