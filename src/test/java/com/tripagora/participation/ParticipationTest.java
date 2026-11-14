package com.tripagora.participation;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 참가 신청과 참가 취소 (여행 상태·역할에 따라)
class ParticipationTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TEMPLATE = 1L;
    private static final long TRIP = 1L;
    private static final String NOT_RECRUITING_MESSAGE = "인원을 모집중인 여행이 아닙니다.";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.template(TEMPLATE, GUIDE, 2);
    }

    @Test
    @DisplayName("모집중 여행에 신청하면 참가자가 되고 인원이 1 늘어난다")
    void appliesForRecruitingTrip() throws Exception {
        trip(TripStatus.RECRUITING, today.plusDays(10));

        apply()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentParticipants").value(1));

        assertThat(travelerParticipations()).isEqualTo(1);
        assertThat(currentParticipants()).isEqualTo(1);
    }

    @Test
    @DisplayName("모집중 여행의 참가를 취소하면 참가 기록이 지워지고 인원이 1 줄어든다")
    void cancelsParticipationOfRecruitingTrip() throws Exception {
        trip(TripStatus.RECRUITING, today.plusDays(10));
        travelerJoined();

        cancel().andExpect(status().isOk());

        assertThat(travelerParticipations()).isZero();
        assertThat(currentParticipants()).isZero();
    }

    @Test
    @DisplayName("모집을 마감한 여행에는 신청할 수 없다")
    void cannotApplyForClosedTrip() throws Exception {
        trip(TripStatus.RECRUITMENT_CLOSED, today.plusDays(10));

        apply()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(NOT_RECRUITING_MESSAGE));

        assertThat(travelerParticipations()).isZero();
    }

    @Test
    @DisplayName("완료된 여행에는 신청할 수 없다")
    void cannotApplyForCompletedTrip() throws Exception {
        trip(TripStatus.COMPLETED, today.minusDays(10));

        apply()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(NOT_RECRUITING_MESSAGE));

        assertThat(travelerParticipations()).isZero();
        assertThat(currentParticipants()).isZero();
    }

    @Test
    @DisplayName("진행 중인 여행의 참가는 취소할 수 없다")
    void cannotCancelParticipationOfInProgressTrip() throws Exception {
        trip(TripStatus.IN_PROGRESS, today);
        travelerJoined();

        cancel()
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인원 모집중 혹은 종료된 여행만 참여 취소를 할 수 있습니다."));

        assertThat(travelerParticipations()).isEqualTo(1);
        assertThat(currentParticipants()).isEqualTo(1);
    }

    @Test
    @DisplayName("가이드는 자기 여행의 참가를 취소할 수 없다")
    void guideCannotCancelOwnTrip() throws Exception {
        trip(TripStatus.RECRUITING, today.plusDays(10));
        travelerJoined();

        mockMvc.perform(delete("/api/participation/trips/" + TRIP).header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("가이드는 자신이 개설한 여행의 참여를 취소할 수 없습니다."));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM participation WHERE trip_id = ? AND role = 'GUIDE'", Integer.class, TRIP))
                .isEqualTo(1);
        assertThat(currentParticipants()).isEqualTo(1);
    }

    // 하루짜리 여행 (가이드는 참가 행이 있지만 인원 수에는 들어가지 않는다)
    private void trip(TripStatus status, LocalDate startDate) {
        fixture.trip(TRIP, TEMPLATE, status, startDate, startDate);
        fixture.participation(1L, TRIP, GUIDE, Role.GUIDE);
    }

    private void travelerJoined() {
        fixture.participation(2L, TRIP, TRAVELER, Role.TRAVELER);
        jdbcTemplate.update("UPDATE trip SET current_participants = 1 WHERE id = ?", TRIP);
    }

    private ResultActions apply() throws Exception {
        return mockMvc.perform(post("/api/participation/trips/" + TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)));
    }

    private ResultActions cancel() throws Exception {
        return mockMvc.perform(delete("/api/participation/trips/" + TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)));
    }

    private int travelerParticipations() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM participation WHERE trip_id = ? AND user_id = ?", Integer.class, TRIP, TRAVELER);
    }

    private int currentParticipants() {
        return jdbcTemplate.queryForObject("SELECT current_participants FROM trip WHERE id = ?", Integer.class, TRIP);
    }
}
