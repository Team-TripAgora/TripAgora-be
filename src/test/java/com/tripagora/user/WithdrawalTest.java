package com.tripagora.user;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 회원 탈퇴: 진행 중이거나 예정된 여행이 있으면 막고, 탈퇴하면 개인정보만 지운다 (남이 보는 이력은 남긴다)
class WithdrawalTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TEMPLATE = 1L;
    private static final long TRIP = 1L;
    private static final String ACTIVE_TRIP_MESSAGE = "진행 중이거나 예정된 여행이 있어 탈퇴할 수 없습니다.";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.template(TEMPLATE, GUIDE, 2);
    }

    @ParameterizedTest(name = "{displayName}: {0}")
    @EnumSource(value = TripStatus.class, names = {"RECRUITING", "RECRUITMENT_CLOSED", "IN_PROGRESS"})
    @DisplayName("진행 중이거나 예정된 여행에 참가 중이면 탈퇴할 수 없다")
    void travelerWithActiveTripCannotWithdraw(TripStatus status) throws Exception {
        tripWithParticipants(status);

        withdraw(TRAVELER, Role.TRAVELER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ACTIVE_TRIP_MESSAGE));

        assertThat(socialIdOf(TRAVELER)).isEqualTo("social-" + TRAVELER);
    }

    @Test
    @DisplayName("모집 중인 여행을 연 가이드는 탈퇴할 수 없다")
    void guideWithRecruitingTripCannotWithdraw() throws Exception {
        tripWithParticipants(TripStatus.RECRUITING);

        withdraw(GUIDE, Role.GUIDE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ACTIVE_TRIP_MESSAGE));

        assertThat(rowCount("SELECT COUNT(*) FROM trip WHERE id = " + TRIP)).isEqualTo(1);
    }

    // 가이드가 연 여행에 여행객이 참가한 상태 (가이드도 여행을 열 때 참가 기록이 생긴다)
    private void tripWithParticipants(TripStatus status) {
        LocalDate start = status == TripStatus.COMPLETED || status == TripStatus.IN_PROGRESS ? today.minusDays(1) : today.plusDays(10);
        LocalDate end = status == TripStatus.COMPLETED ? today.minusDays(1) : start.plusDays(1);
        fixture.trip(TRIP, TEMPLATE, status, start, end);
        fixture.participation(1L, TRIP, GUIDE, Role.GUIDE);
        fixture.participation(2L, TRIP, TRAVELER, Role.TRAVELER);
    }

    private ResultActions withdraw(long userId, Role role) throws Exception {
        return mockMvc.perform(delete("/api/users/me").header("Authorization", bearer(userId, role)));
    }

    private String socialIdOf(long userId) {
        return jdbcTemplate.queryForObject("SELECT social_id FROM users WHERE id = ?", String.class, userId);
    }

    private int rowCount(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
