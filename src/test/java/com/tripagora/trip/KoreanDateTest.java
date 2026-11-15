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

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// '오늘'은 서버 시간대와 상관없이 한국 날짜다. 시계를 UTC 3월 9일 15:30(= 한국 3월 10일 0:30)에 고정해,
// UTC 날짜(9일)와 한국 날짜(10일)가 다른 순간에 한국 날짜로 판단하는지 본다
class KoreanDateTest extends IntegrationTest {
    private static final Instant KOREA_00_30 = Instant.parse("2026-03-09T15:30:00Z");
    private static final LocalDate KOREAN_TODAY = LocalDate.of(2026, 3, 10);
    private static final long GUIDE = 1L;
    private static final long TEMPLATE = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TripScheduler tripScheduler;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.template(TEMPLATE, GUIDE, 1);
        clock.fixAt(KOREA_00_30);
    }

    @Test
    @DisplayName("한국 시간 0시 30분, 스케줄러는 한국 날짜로 오늘 시작하는 모집 마감 여행을 시작한다")
    void schedulerStartsTripsByKoreanDate() {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITMENT_CLOSED, KOREAN_TODAY, KOREAN_TODAY);

        tripScheduler.startTrips();

        assertThat(statusOf(1L)).isEqualTo(TripStatus.IN_PROGRESS.name());
    }

    @Test
    @DisplayName("스케줄러가 못 돈 날이 있어도, 시작일이 지난 모집 마감 여행은 다음 실행에서 시작한다")
    void schedulerCatchesUpMissedStartDates() {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITMENT_CLOSED, KOREAN_TODAY.minusDays(1), KOREAN_TODAY.plusDays(1));

        tripScheduler.startTrips();

        assertThat(statusOf(1L)).isEqualTo(TripStatus.IN_PROGRESS.name());
    }

    @Test
    @DisplayName("한국 시간 0시 30분, 스케줄러는 한국 날짜로 어제 끝난 여행을 완료한다")
    void schedulerCompletesTripsByKoreanDate() {
        fixture.trip(1L, TEMPLATE, TripStatus.IN_PROGRESS, KOREAN_TODAY.minusDays(2), KOREAN_TODAY.minusDays(1));

        tripScheduler.completeTrips();

        assertThat(statusOf(1L)).isEqualTo(TripStatus.COMPLETED.name());
    }

    @Test
    @DisplayName("한국 시간 0시 30분에 모집을 마감하면, 한국 날짜로 오늘 시작하는 여행은 바로 진행 중이 된다")
    void closingRecruitmentUsesKoreanDate() throws Exception {
        fixture.trip(1L, TEMPLATE, TripStatus.RECRUITING, KOREAN_TODAY, KOREAN_TODAY);

        mockMvc.perform(post("/api/trips/1/close").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk());

        assertThat(statusOf(1L)).isEqualTo(TripStatus.IN_PROGRESS.name());
    }

    @Test
    @DisplayName("여행 시작일 검증도 한국 날짜 기준이다: 한국 날짜로 오늘은 통과, 어제는 400")
    void startDateValidationUsesKoreanDate() throws Exception {
        createTrip(KOREAN_TODAY).andExpect(status().isOk());

        createTrip(KOREAN_TODAY.minusDays(1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].reason").value("여행 시작일은 오늘 이후여야 합니다."));
    }

    private ResultActions createTrip(LocalDate startDate) throws Exception {
        return mockMvc.perform(post("/api/trips")
                .header("Authorization", bearer(GUIDE, Role.GUIDE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"templateId\":" + TEMPLATE + ",\"maxParticipants\":4,\"startDate\":\"" + startDate + "\"}"));
    }

    private String statusOf(long tripId) {
        return jdbcTemplate.queryForObject("SELECT status FROM trip WHERE id = ?", String.class, tripId);
    }
}
