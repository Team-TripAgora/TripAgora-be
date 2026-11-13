package com.tripagora.template;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 일정 '전체 교체': 템플릿 일정과 진행 중 여행 일정이 요청한 일정으로 바뀐다 (같은 요청 형식을 쓴다)
class ItineraryUpdateTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final String NEW_ITINERARIES = "{\"itineraries\":["
            + "{\"day\":1,\"location\":\"경복궁\",\"content\":\"고궁 산책\",\"startTime\":\"09:00:00\",\"latitude\":37.57,\"longitude\":126.97},"
            + "{\"day\":2,\"location\":\"남산타워\",\"content\":\"야경\",\"startTime\":\"19:00:00\",\"latitude\":37.55,\"longitude\":126.98}]}";

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
    }

    @Test
    @DisplayName("템플릿 일정을 새 일정으로 바꾼다")
    void replacesTemplateItineraries() throws Exception {
        fixture.template(1L, GUIDE, 3);

        mockMvc.perform(put("/api/templates/1/itineraries")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_ITINERARIES))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/templates/1/itineraries").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(jsonPath("$.data.itineraries", hasSize(2)))
                .andExpect(jsonPath("$.data.itineraries[*].location", contains("경복궁", "남산타워")))
                .andExpect(jsonPath("$.data.itineraries[0].content").value("고궁 산책"))
                .andExpect(jsonPath("$.data.itineraries[1].startTime").value("19:00:00"));
    }

    @Test
    @DisplayName("진행 중 여행의 일정을 새 일정으로 바꾼다")
    void replacesTripItineraries() throws Exception {
        LocalDate today = LocalDate.now();
        fixture.template(1L, GUIDE, 2);
        fixture.trip(1L, 1L, TripStatus.IN_PROGRESS, today.minusDays(1), today);
        fixture.tripItinerary(1L, 1);
        fixture.participation(1L, 1L, GUIDE, Role.GUIDE);

        mockMvc.perform(put("/api/trips/1/itineraries")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_ITINERARIES))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trips/1/itineraries").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(jsonPath("$.data.itineraries", hasSize(2)))
                .andExpect(jsonPath("$.data.itineraries[*].location", contains("경복궁", "남산타워")));
    }
}
