package com.tripagora.review;

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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 리뷰를 쓰고·고치고·지울 때 다시 계산하는 평점
//   템플릿: 리뷰 수, 평점 평균 (평점은 필수라 평점 없는 리뷰는 없다)
//   가이드: 템플릿 리뷰 수의 합, 리뷰가 있는 템플릿 평균들의 평균
class RatingTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TEMPLATE_A = 1L;
    private static final long TEMPLATE_B = 2L;
    private static final long TEMPLATE_WITHOUT_REVIEW = 3L;
    private static final long TRIP_A1 = 1L;
    private static final long TRIP_A2 = 2L;
    private static final long TRIP_B = 3L;
    private static final long REVIEW_B = 13L;
    // 소수점이 끝나지 않는 평균도 그대로(DB 소수 4자리로 잘리지 않게) 계산하는지 본다
    private static final double PRECISION = 1e-9;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LocalDate lastWeek = today().minusDays(7);
        fixture.guide(GUIDE);
        for (long traveler = 2; traveler <= 6; traveler++) {
            fixture.traveler(traveler);
        }
        fixture.template(TEMPLATE_A, GUIDE, 1);
        fixture.template(TEMPLATE_B, GUIDE, 1);
        fixture.template(TEMPLATE_WITHOUT_REVIEW, GUIDE, 1);
        fixture.trip(TRIP_A1, TEMPLATE_A, TripStatus.COMPLETED, lastWeek, lastWeek);
        fixture.trip(TRIP_A2, TEMPLATE_A, TripStatus.COMPLETED, lastWeek, lastWeek);
        fixture.trip(TRIP_B, TEMPLATE_B, TripStatus.COMPLETED, lastWeek, lastWeek);
        fixture.participation(2L, TRIP_A1, 2L, Role.TRAVELER);
        fixture.participation(4L, TRIP_A1, 4L, Role.TRAVELER);
        fixture.participation(5L, TRIP_B, 5L, Role.TRAVELER);
        fixture.participation(6L, TRIP_A2, 6L, Role.TRAVELER);

        // 템플릿 A: 5점, 4점 / 템플릿 B: 2점 / 템플릿 C: 리뷰 없음
        review(11L, 2L, TRIP_A1, TEMPLATE_A, 5);
        review(14L, 4L, TRIP_A1, TEMPLATE_A, 4);
        review(REVIEW_B, 5L, TRIP_B, TEMPLATE_B, 2);
        jdbcTemplate.update("UPDATE template SET avg_rating = 4.5, review_count = 2 WHERE id = ?", TEMPLATE_A);
        jdbcTemplate.update("UPDATE template SET avg_rating = 2.0, review_count = 1 WHERE id = ?", TEMPLATE_B);
    }

    @Test
    @DisplayName("리뷰를 쓰면 템플릿은 리뷰 평균을, 가이드는 리뷰 있는 템플릿 평균들의 평균을 다시 낸다")
    void createReviewRecalculatesRatings() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(6L, Role.TRAVELER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":" + TRIP_A2 + ",\"content\":\"좋았어요\",\"rating\":4}"))
                .andExpect(status().isOk());

        double templateAverage = (5 + 4 + 4) / 3.0;
        assertTemplate(TEMPLATE_A, templateAverage, 3);
        assertTemplate(TEMPLATE_B, 2.0, 1);
        assertGuide((templateAverage + 2.0) / 2, 4);
    }

    @Test
    @DisplayName("리뷰 평점을 고치면 템플릿과 가이드 평점을 다시 계산한다")
    void updateReviewRecalculatesRatings() throws Exception {
        mockMvc.perform(put("/api/reviews/" + REVIEW_B)
                        .header("Authorization", bearer(5L, Role.TRAVELER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"다시 생각해 보니 괜찮았어요\",\"rating\":5}"))
                .andExpect(status().isOk());

        assertTemplate(TEMPLATE_B, 5.0, 1);
        assertGuide((4.5 + 5.0) / 2, 3);
    }

    @Test
    @DisplayName("템플릿의 마지막 리뷰를 지우면 그 템플릿은 0점·0개가 되고 가이드 평균에서 빠진다")
    void deleteLastReviewOfTemplate() throws Exception {
        mockMvc.perform(delete("/api/reviews/" + REVIEW_B).header("Authorization", bearer(5L, Role.TRAVELER)))
                .andExpect(status().isOk());

        assertTemplate(TEMPLATE_B, 0.0, 0);
        assertTemplate(TEMPLATE_WITHOUT_REVIEW, 0.0, 0);
        assertGuide(4.5, 2);
    }

    private void review(long id, long authorId, long tripId, long templateId, int rating) {
        fixture.review(id, authorId, tripId, templateId, GUIDE);
        jdbcTemplate.update("UPDATE review SET rating = ? WHERE id = ?", rating, id);
    }

    private void assertTemplate(long templateId, double average, int count) {
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT avg_rating, review_count FROM template WHERE id = ?", templateId);
        assertThat(((Number) row.get("avg_rating")).doubleValue()).isCloseTo(average, within(PRECISION));
        assertThat(((Number) row.get("review_count")).intValue()).isEqualTo(count);
    }

    private void assertGuide(double average, int count) {
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT total_avg_rating, total_review_count FROM guide_profile WHERE id = ?", GUIDE);
        assertThat(((Number) row.get("total_avg_rating")).doubleValue()).isCloseTo(average, within(PRECISION));
        assertThat(((Number) row.get("total_review_count")).intValue()).isEqualTo(count);
    }
}
