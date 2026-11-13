package com.tripagora.trip;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 내 여행 목록의 상태 필터, 완료된 여행·여행 상세의 리뷰 id
class TripQueryTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TEMPLATE = 1L;
    private static final long REVIEWED_TRIP = 4L;
    private static final long NOT_REVIEWED_TRIP = 5L;
    private static final long REVIEW = 10L;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.template(TEMPLATE, GUIDE, 2);
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITMENT_CLOSED, today.plusDays(30), today.plusDays(31));
        fixture.trip(2L, TEMPLATE, TripStatus.IN_PROGRESS, today.minusDays(1), today);
        fixture.trip(3L, TEMPLATE, TripStatus.RECRUITING, today.plusDays(60), today.plusDays(61));
        fixture.trip(REVIEWED_TRIP, TEMPLATE, TripStatus.COMPLETED, today.minusDays(60), today.minusDays(59));
        fixture.trip(NOT_REVIEWED_TRIP, TEMPLATE, TripStatus.COMPLETED, today.minusDays(40), today.minusDays(39));
        fixture.participation(1L, REVIEWED_TRIP, TRAVELER, Role.TRAVELER);
        fixture.participation(2L, NOT_REVIEWED_TRIP, TRAVELER, Role.TRAVELER);
        fixture.review(REVIEW, TRAVELER, REVIEWED_TRIP, TEMPLATE, GUIDE);
    }

    @Test
    @DisplayName("상태를 지정하지 않으면 내가 만든 여행 전체를 준다")
    void myTripsWithoutStatusReturnsAll() throws Exception {
        mockMvc.perform(get("/api/trips/me").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips", hasSize(5)));
    }

    @Test
    @DisplayName("상태를 빈 값으로 보내도 내가 만든 여행 전체를 준다")
    void myTripsWithEmptyStatusReturnsAll() throws Exception {
        mockMvc.perform(get("/api/trips/me").param("statuses", "").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips", hasSize(5)));
    }

    @Test
    @DisplayName("상태를 지정하면 그 상태의 여행만 준다")
    void myTripsWithStatusFilters() throws Exception {
        mockMvc.perform(get("/api/trips/me").param("statuses", "RECRUITING").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips[*].tripId", contains(3)));
    }

    @Test
    @DisplayName("완료된 여행 목록에 내가 쓴 리뷰 id를 준다")
    void completedTripsContainReviewId() throws Exception {
        mockMvc.perform(get("/api/trips/completed").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 4)].reviewId", contains((int) REVIEW)))
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 4)].hasWrittenReview", contains(true)))
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 5)].reviewId", contains(nullValue())))
                .andExpect(jsonPath("$.data.trips[?(@.tripId == 5)].hasWrittenReview", contains(false)));
    }

    @Test
    @DisplayName("리뷰를 쓴 여행의 상세에 리뷰 id를 준다")
    void tripDetailContainsReviewId() throws Exception {
        mockMvc.perform(get("/api/trips/" + REVIEWED_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewId").value(REVIEW))
                .andExpect(jsonPath("$.data.hasWrittenReview").value(true));
    }

    @Test
    @DisplayName("리뷰를 쓰지 않은 여행의 상세에는 리뷰 id가 null이다")
    void tripDetailWithoutReviewHasNullReviewId() throws Exception {
        mockMvc.perform(get("/api/trips/" + NOT_REVIEWED_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewId").value(nullValue()))
                .andExpect(jsonPath("$.data.hasWrittenReview").value(false));
    }
}
