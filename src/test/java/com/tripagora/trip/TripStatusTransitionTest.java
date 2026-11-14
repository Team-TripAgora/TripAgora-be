package com.tripagora.trip;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.trip.scheduler.TripScheduler;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 여행 상태 전이(모집 마감, 스케줄러의 시작·완료)와 상태에 따라 막히는 요청
class TripStatusTransitionTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TEMPLATE = 1L;
    private static final String NOT_RECRUITING_MESSAGE = "인원을 모집중인 여행이 아닙니다.";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TripScheduler tripScheduler;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.template(TEMPLATE, GUIDE, 2);
    }

    @Test
    @DisplayName("시작일이 된 여행의 모집을 마감하면 바로 진행 중이 되고 채팅방이 생긴다")
    void closingStartedTripMakesItInProgress() throws Exception {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITING, today, today.plusDays(1));

        closeRecruitment(1L).andExpect(status().isOk());

        assertThat(statusOf(1L)).isEqualTo(TripStatus.IN_PROGRESS.name());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM room WHERE trip_id = 1", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("종료일이 지난 여행은 모집을 마감할 수 없다")
    void cannotCloseEndedTrip() throws Exception {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITING, today.minusDays(3), today.minusDays(2));

        closeRecruitment(1L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("여행 종료날짜가 이미 지났습니다. 여행을 새로 개설하세요."));

        assertThat(statusOf(1L)).isEqualTo(TripStatus.RECRUITING.name());
    }

    @Test
    @DisplayName("모집중이 아닌 여행은 모집을 마감할 수 없다")
    void cannotCloseTripThatIsNotRecruiting() throws Exception {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITMENT_CLOSED, today.plusDays(10), today.plusDays(11));

        closeRecruitment(1L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(NOT_RECRUITING_MESSAGE));
    }

    @Test
    @DisplayName("스케줄러는 시작일이 된 모집 마감 여행을 진행 중으로 바꾼다")
    void schedulerStartsClosedTripsOnStartDate() {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITMENT_CLOSED, today, today.plusDays(1));
        fixture.trip(2L, TEMPLATE, TripStatus.RECRUITMENT_CLOSED, today.plusDays(1), today.plusDays(2));
        fixture.trip(3L, TEMPLATE, TripStatus.RECRUITING, today, today.plusDays(1));

        tripScheduler.startTrips();

        assertThat(statusOf(1L)).isEqualTo(TripStatus.IN_PROGRESS.name());
        assertThat(statusOf(2L)).isEqualTo(TripStatus.RECRUITMENT_CLOSED.name());
        assertThat(statusOf(3L)).isEqualTo(TripStatus.RECRUITING.name());
    }

    @Test
    @DisplayName("스케줄러는 종료일이 지난 진행 중 여행을 완료로 바꾼다")
    void schedulerCompletesInProgressTripsAfterEndDate() {
        fixture.trip(1L, TEMPLATE, TripStatus.IN_PROGRESS, today.minusDays(2), today.minusDays(1));
        fixture.trip(2L, TEMPLATE, TripStatus.IN_PROGRESS, today.minusDays(1), today);

        tripScheduler.completeTrips();

        assertThat(statusOf(1L)).isEqualTo(TripStatus.COMPLETED.name());
        assertThat(statusOf(2L)).isEqualTo(TripStatus.IN_PROGRESS.name());
    }

    @Test
    @DisplayName("모집중이 아닌 여행은 수정할 수 없다")
    void cannotUpdateTripThatIsNotRecruiting() throws Exception {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITMENT_CLOSED, today.plusDays(10), today.plusDays(11));

        mockMvc.perform(patch("/api/trips/1")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxParticipants\":4,\"startDate\":\"" + today.plusDays(10) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(NOT_RECRUITING_MESSAGE));
    }

    @Test
    @DisplayName("진행 중이 아닌 여행의 일정은 바꿀 수 없다")
    void cannotEditItinerariesOfTripThatIsNotInProgress() throws Exception {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITING, today.plusDays(10), today.plusDays(11));
        fixture.participation(1L, 1L, GUIDE, Role.GUIDE);

        mockMvc.perform(put("/api/trips/1/itineraries")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itineraries\":[{\"day\":1,\"location\":\"경복궁\",\"content\":\"고궁 산책\","
                                + "\"startTime\":\"09:00:00\",\"latitude\":37.57,\"longitude\":126.97}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("진행중인 여행에만 가능한 요청입니다."));
    }

    @Test
    @DisplayName("끝나지 않은 여행에는 리뷰를 쓸 수 없다")
    void cannotReviewTripThatIsNotCompleted() throws Exception {
        fixture.trip(1L, TEMPLATE, TripStatus.IN_PROGRESS, today.minusDays(1), today);
        fixture.participation(1L, 1L, TRAVELER, Role.TRAVELER);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(TRAVELER, Role.TRAVELER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":1,\"content\":\"좋았어요\",\"rating\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("종료된 여행에 대해서만 리뷰를 작성할 수 있습니다."));
    }

    private ResultActions closeRecruitment(long tripId) throws Exception {
        return mockMvc.perform(post("/api/trips/" + tripId + "/close").header("Authorization", bearer(GUIDE, Role.GUIDE)));
    }

    private String statusOf(long tripId) {
        return jdbcTemplate.queryForObject("SELECT status FROM trip WHERE id = ?", String.class, tripId);
    }
}
