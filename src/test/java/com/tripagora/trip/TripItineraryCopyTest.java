package com.tripagora.trip;

import com.jayway.jsonpath.JsonPath;
import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 여행을 열면 템플릿 일정을 복사해 여행이 따로 갖는다. 그 뒤 템플릿 일정을 바꿔도 여행 일정은 바뀌지 않는다
class TripItineraryCopyTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TEMPLATE = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.template(TEMPLATE, GUIDE, 3);
    }

    @Test
    @DisplayName("여행을 열면 템플릿 일정이 여행 일정으로 복사된다")
    void copiesTemplateItinerariesWhenTripIsCreated() throws Exception {
        long tripId = createTrip();

        mockMvc.perform(get("/api/trips/" + tripId + "/itineraries").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itineraries[*].day", contains(1, 2, 3)))
                .andExpect(jsonPath("$.data.itineraries[*].location", contains("장소1", "장소2", "장소3")))
                .andExpect(jsonPath("$.data.itineraries[*].content", contains("1일차", "2일차", "3일차")))
                .andExpect(jsonPath("$.data.itineraries[0].startTime").value("10:00:00"))
                .andExpect(jsonPath("$.data.itineraries[0].latitude").value(37.5))
                .andExpect(jsonPath("$.data.itineraries[0].longitude").value(127.0));
    }

    @Test
    @DisplayName("여행을 연 뒤 템플릿 일정을 바꿔도 여행 일정은 그대로다")
    void keepsTripItinerariesWhenTemplateItinerariesChange() throws Exception {
        long tripId = createTrip();
        // 모집·진행 중 여행이 있으면 템플릿을 고칠 수 없으므로, 여행을 끝난 상태로 둔다
        jdbcTemplate.update("UPDATE trip SET status = ? WHERE id = ?", TripStatus.COMPLETED.name(), tripId);

        mockMvc.perform(put("/api/templates/" + TEMPLATE + "/itineraries")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itineraries\":[{\"day\":1,\"location\":\"경복궁\",\"content\":\"고궁 산책\","
                                + "\"startTime\":\"09:00:00\",\"latitude\":37.57,\"longitude\":126.97}]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/templates/" + TEMPLATE + "/itineraries").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(jsonPath("$.data.itineraries[*].location", contains("경복궁")));
        mockMvc.perform(get("/api/trips/" + tripId + "/itineraries").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(jsonPath("$.data.itineraries[*].location", contains("장소1", "장소2", "장소3")));
    }

    private long createTrip() throws Exception {
        String response = mockMvc.perform(post("/api/trips")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":" + TEMPLATE + ",\"maxParticipants\":4,\"startDate\":\"" + LocalDate.now().plusDays(30) + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.data.tripId")).longValue();
    }
}
