package com.tripagora.trip;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 여행 검색의 쿼리 파라미터 (검색어, 기간, 지역, 태그). 같은 이름을 여러 번 보내거나 쉼표로 이어 보내도 된다
class TripSearchTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.region(1L, "서울");
        fixture.region(2L, "부산");
        fixture.region(3L, "제주");
        fixture.tag(1L, "맛집");
        fixture.tag(2L, "역사");
        fixture.tag(3L, "자연");

        // 여행 n: 템플릿 n, 시작일 오늘 + 10n일 (당일 여행)
        trip(1L, 1L, 1L);
        trip(2L, 2L, 2L);
        trip(3L, 1L, 2L);
        trip(4L, 3L, 3L);
        fixture.template(5L, GUIDE, 1);
        fixture.trip(5L, 5L, TripStatus.RECRUITMENT_CLOSED, today.plusDays(50), today.plusDays(50));
    }

    @Test
    @DisplayName("조건이 없으면 모집 중인 여행을 모두 준다")
    void returnsAllRecruitingTrips() throws Exception {
        search(get("/api/trips/search"))
                .andExpect(jsonPath("$.data.trips[*].tripId", containsInAnyOrder(1, 2, 3, 4)));
    }

    @Test
    @DisplayName("검색어로 템플릿 제목을 찾는다")
    void searchesByKeyword() throws Exception {
        search(get("/api/trips/search").param("keyword", "플랜2"))
                .andExpect(jsonPath("$.data.trips[*].tripId", containsInAnyOrder(2)));
    }

    @Test
    @DisplayName("기간과 겹치는 여행을 찾는다")
    void searchesByDateRange() throws Exception {
        search(get("/api/trips/search")
                .param("searchStartDate", today.plusDays(15).toString())
                .param("searchEndDate", today.plusDays(35).toString()))
                .andExpect(jsonPath("$.data.trips[*].tripId", containsInAnyOrder(2, 3)));
    }

    @Test
    @DisplayName("시작일만 보내면 그날 이후에 시작하는 여행을 찾는다")
    void searchesByStartDateOnly() throws Exception {
        search(get("/api/trips/search").param("searchStartDate", today.plusDays(25).toString()))
                .andExpect(jsonPath("$.data.trips[*].tripId", containsInAnyOrder(3, 4)));
    }

    @Test
    @DisplayName("지역을 같은 이름으로 여러 번 보내면 그중 하나라도 맞는 여행을 찾는다")
    void searchesByRepeatedRegionIds() throws Exception {
        search(get("/api/trips/search").param("regionIds", "1", "2"))
                .andExpect(jsonPath("$.data.trips[*].tripId", containsInAnyOrder(1, 2, 3)));
    }

    @Test
    @DisplayName("태그를 쉼표로 이어 보내면 그중 하나라도 맞는 여행을 찾는다")
    void searchesByCommaSeparatedTagIds() throws Exception {
        search(get("/api/trips/search").param("tagIds", "1,2"))
                .andExpect(jsonPath("$.data.trips[*].tripId", containsInAnyOrder(1, 2, 3)));
    }

    @Test
    @DisplayName("지역과 태그를 함께 보내면 둘 다 맞는 여행만 찾는다")
    void searchesByRegionAndTag() throws Exception {
        search(get("/api/trips/search").param("regionIds", "1").param("tagIds", "2"))
                .andExpect(jsonPath("$.data.trips[*].tripId", containsInAnyOrder(3)));
    }

    // 템플릿 id = 여행 id, 제목 "플랜{id}", 지역·태그 하나씩
    private void trip(long id, long regionId, long tagId) {
        fixture.template(id, GUIDE, 1);
        fixture.templateRegion(id, regionId);
        fixture.templateTag(id, tagId);
        LocalDate startDate = today.plusDays(10 * id);
        fixture.trip(id, id, TripStatus.RECRUITING, startDate, startDate);
    }

    private ResultActions search(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk());
    }
}
