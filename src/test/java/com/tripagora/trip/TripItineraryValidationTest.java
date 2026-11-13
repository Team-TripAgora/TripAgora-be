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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 진행 중인 여행의 일정 수정도 템플릿과 같은 요청 형식을 쓴다
class TripItineraryValidationTest extends IntegrationTest {

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        fixture.guide(1L);
        fixture.template(1L, 1L, 2);
        fixture.trip(1L, 1L, TripStatus.IN_PROGRESS, today.minusDays(1), today);
        fixture.participation(1L, 1L, 1L, Role.GUIDE);
    }

    @Test
    @DisplayName("진행 중 여행의 일정 목록이 없으면 400")
    void rejectsMissingItineraries() throws Exception {
        updateItineraries("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("itineraries"));
    }

    @Test
    @DisplayName("진행 중 여행의 일정 장소가 255자를 넘으면 400")
    void rejectsItineraryLocationOver255Characters() throws Exception {
        updateItineraries("{\"itineraries\":[{\"day\":1,\"location\":\"" + "a".repeat(256) + "\",\"content\":\"내용\","
                + "\"startTime\":\"10:00:00\",\"latitude\":37.5,\"longitude\":127.0}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].reason").value("일정 장소는 255자 이하여야 합니다."));
    }

    private ResultActions updateItineraries(String body) throws Exception {
        return mockMvc.perform(put("/api/trips/1/itineraries")
                .header("Authorization", bearer(1L, Role.GUIDE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
