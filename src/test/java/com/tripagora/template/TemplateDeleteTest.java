package com.tripagora.template;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 템플릿 삭제: 여행을 한 번이라도 연 템플릿은 지우지 않는다 (지우면 그 여행의 참가·리뷰·채팅 이력이 함께 사라진다)
class TemplateDeleteTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TEMPLATE = 1L;
    private static final long TRIP = 1L;
    private static final String HAS_TRIPS_MESSAGE = "해당 플랜으로 개설된 여행이 존재하여 수정 또는 삭제할 수 없습니다.";

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
    @DisplayName("완료된 여행이 있는 템플릿은 지울 수 없고, 여행객의 참가·리뷰가 남는다")
    void cannotDeleteTemplateWithCompletedTrip() throws Exception {
        completedTripWithReview();

        deleteTemplate()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(HAS_TRIPS_MESSAGE));

        assertThat(rowCount("SELECT COUNT(*) FROM template WHERE id = " + TEMPLATE)).isEqualTo(1);
        assertThat(rowCount("SELECT COUNT(*) FROM participation WHERE user_id = " + TRAVELER)).isEqualTo(1);
        assertThat(rowCount("SELECT COUNT(*) FROM review WHERE author_id = " + TRAVELER)).isEqualTo(1);
    }

    @Test
    @DisplayName("채팅이 있었던 완료 여행의 템플릿도 지울 수 없다")
    void cannotDeleteTemplateWithChatHistory() throws Exception {
        completedTripWithReview();
        fixture.room(1L, TRIP);
        fixture.chatMessage(1L, TRAVELER, Role.TRAVELER);

        deleteTemplate()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(HAS_TRIPS_MESSAGE));
    }

    @Test
    @DisplayName("모집 중인 여행이 있는 템플릿은 지울 수 없다")
    void cannotDeleteTemplateWithRecruitingTrip() throws Exception {
        fixture.trip(TRIP, TEMPLATE, TripStatus.RECRUITING, today.plusDays(10), today.plusDays(11));

        deleteTemplate().andExpect(status().isConflict());
    }

    @Test
    @DisplayName("여행을 한 번도 열지 않은 템플릿은 지운다")
    void deletesTemplateWithoutTrips() throws Exception {
        deleteTemplate().andExpect(status().isOk());

        assertThat(rowCount("SELECT COUNT(*) FROM template WHERE id = " + TEMPLATE)).isZero();
    }

    private void completedTripWithReview() {
        fixture.trip(TRIP, TEMPLATE, TripStatus.COMPLETED, today.minusDays(10), today.minusDays(9));
        fixture.participation(1L, TRIP, GUIDE, Role.GUIDE);
        fixture.participation(2L, TRIP, TRAVELER, Role.TRAVELER);
        fixture.review(1L, TRAVELER, TRIP, TEMPLATE, GUIDE);
    }

    private ResultActions deleteTemplate() throws Exception {
        return mockMvc.perform(delete("/api/templates/" + TEMPLATE).header("Authorization", bearer(GUIDE, Role.GUIDE)));
    }

    private int rowCount(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
