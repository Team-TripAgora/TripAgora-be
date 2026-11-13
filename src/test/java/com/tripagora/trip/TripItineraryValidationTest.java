package com.tripagora.trip;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 진행 중인 여행의 일정 수정도 템플릿과 같은 요청 형식을 쓴다
class TripItineraryValidationTest extends IntegrationTest {

    @Test
    @DisplayName("진행 중 여행의 일정 목록이 없으면 400")
    void rejectsMissingItineraries() throws Exception {
        LocalDate today = LocalDate.now();
        fixture.guide(1L);
        fixture.template(1L, 1L, 2);
        fixture.trip(1L, 1L, TripStatus.IN_PROGRESS, today.minusDays(1), today);
        fixture.participation(1L, 1L, 1L, Role.GUIDE);

        mockMvc.perform(put("/api/trips/1/itineraries")
                        .header("Authorization", bearer(1L, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("itineraries"));
    }
}
