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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 모집은 시작 전날까지다. 시작일이 오늘이거나 지난 모집 중 여행은 검색에 나오지 않고 신청할 수 없다
class RecruitmentPeriodTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long STARTED_YESTERDAY = 1L;
    private static final long STARTS_TODAY = 2L;
    private static final long STARTS_TOMORROW = 3L;

    @BeforeEach
    void setUp() {
        LocalDate today = today();
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.template(1L, GUIDE, 2);
        fixture.trip(STARTED_YESTERDAY, 1L, TripStatus.RECRUITING, today.minusDays(1), today);
        fixture.trip(STARTS_TODAY, 1L, TripStatus.RECRUITING, today, today.plusDays(1));
        fixture.trip(STARTS_TOMORROW, 1L, TripStatus.RECRUITING, today.plusDays(1), today.plusDays(2));
    }

    @Test
    @DisplayName("검색에는 시작일이 내일 이후인 모집 중 여행만 나온다")
    void searchShowsOnlyTripsStartingLater() throws Exception {
        mockMvc.perform(get("/api/trips/search").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips[*].tripId", contains((int) STARTS_TOMORROW)));
    }

    @Test
    @DisplayName("시작일이 오늘이거나 지난 모집 중 여행은 신청할 수 없다")
    void cannotJoinTripThatHasStarted() throws Exception {
        for (long tripId : new long[]{STARTED_YESTERDAY, STARTS_TODAY}) {
            join(tripId).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("인원을 모집중인 여행이 아닙니다."));
        }
    }

    @Test
    @DisplayName("시작일이 내일인 모집 중 여행은 신청할 수 있다")
    void canJoinTripStartingTomorrow() throws Exception {
        join(STARTS_TOMORROW).andExpect(status().isOk());
    }

    private ResultActions join(long tripId) throws Exception {
        return mockMvc.perform(post("/api/participation/trips/" + tripId).header("Authorization", bearer(TRAVELER, Role.TRAVELER)));
    }
}
