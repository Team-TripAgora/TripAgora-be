package com.tripagora.trip;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 검색 결과는 시작일이 빠른 순이고, 시작일이 같으면 id 순이다 (페이지를 넘겨도 빠지거나 겹치지 않는다)
class TripSearchOrderTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;

    @BeforeEach
    void setUp() {
        LocalDate sameDay = today().plusDays(20);
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.region(1L, "서울");
        fixture.region(2L, "부산");
        // 시작일이 같은 여행 5개 (여행마다 지역 2개를 둬 지역 조인으로 줄이 불어나는 실제 검색과 같게)와, 더 이른 여행 1개
        for (long id = 1; id <= 6; id++) {
            fixture.template(id, GUIDE, 1);
            fixture.templateRegion(id, 1L);
            fixture.templateRegion(id, 2L);
            LocalDate startDate = (id == 6) ? sameDay.minusDays(5) : sameDay;
            fixture.trip(id, id, TripStatus.RECRUITING, startDate, startDate);
        }
    }

    @Test
    @DisplayName("시작일이 같은 여행이 페이지 경계에 걸려도 id 순으로 이어진다")
    void sameStartDateIsOrderedById() throws Exception {
        searchPage(0).andExpect(jsonPath("$.data.trips[*].tripId", contains(6, 1)))
                .andExpect(jsonPath("$.data.hasNext").value(true));
        searchPage(1).andExpect(jsonPath("$.data.trips[*].tripId", contains(2, 3)))
                .andExpect(jsonPath("$.data.hasNext").value(true));
        searchPage(2).andExpect(jsonPath("$.data.trips[*].tripId", contains(4, 5)))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    private ResultActions searchPage(int page) throws Exception {
        return mockMvc.perform(get("/api/trips/search").param("page", String.valueOf(page)).param("size", "2")
                        .header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk());
    }
}
