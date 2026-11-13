package com.tripagora.querycount;

import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 리뷰 목록과 리뷰 작성(평점 재계산, D-12)의 쿼리 수 기준선
class ReviewQueryCountTest extends QueryCountTest {
    private static final long GUIDE = 1L;
    private static final long AUTHOR = 2L;
    private static final long TRIP = 1L;
    private static final long TEMPLATE = 1L;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.template(TEMPLATE, GUIDE, 2);
        fixture.trip(TRIP, TEMPLATE, TripStatus.COMPLETED, today.minusDays(10), today.minusDays(9));
        fixture.room(1L, TRIP);
        fixture.participation(GUIDE, TRIP, GUIDE, Role.GUIDE);
    }

    // 3번부터 여행객 reviews 명이 완료된 여행에 참가해 리뷰를 하나씩 남긴다 (리뷰 id = 작성자 id)
    private void reviewsByOtherTravelers(int reviews) {
        for (long userId = 3; userId < 3 + reviews; userId++) {
            fixture.traveler(userId);
            fixture.participation(userId, TRIP, userId, Role.TRAVELER);
            fixture.review(userId, userId, TRIP, TEMPLATE, GUIDE);
        }
    }

    @ParameterizedTest(name = "{displayName}: 리뷰 {0}건 → 쿼리 {1}개, 엔티티 {2}개")
    @CsvSource({"5, 7, 11", "10, 12, 21"})
    @DisplayName("템플릿 리뷰 목록 (작성자 모두 다름)")
    void templateReviews(int reviews, long queries, long entities) throws Exception {
        reviewsByOtherTravelers(reviews);
        fixture.traveler(AUTHOR);

        Counts counts = measure(() -> mockMvc.perform(get("/api/reviews/template/" + TEMPLATE).header("Authorization", bearer(AUTHOR, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews", hasSize(reviews))));

        assertCounts(counts, queries, entities);
    }

    @ParameterizedTest(name = "{displayName}: 기존 리뷰 {0}건, 가이드 템플릿 {1}개 → 쿼리 {2}개, 엔티티 {3}개")
    @CsvSource({"5, 2, 11, 12", "10, 4, 11, 19"})
    @DisplayName("리뷰 작성 (템플릿·가이드 평점 재계산)")
    void createReview(int existingReviews, int guideTemplates, long queries, long entities) throws Exception {
        reviewsByOtherTravelers(existingReviews);
        for (long templateId = 2; templateId <= guideTemplates; templateId++) {
            fixture.template(templateId, GUIDE, 2);
        }
        fixture.traveler(AUTHOR);
        fixture.participation(AUTHOR, TRIP, AUTHOR, Role.TRAVELER);

        Counts counts = measure(() -> mockMvc.perform(post("/api/reviews")
                        .header("Authorization", bearer(AUTHOR, Role.TRAVELER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tripId\":" + TRIP + ",\"content\":\"좋았어요\",\"rating\":4}"))
                .andExpect(status().isOk()));

        assertCounts(counts, queries, entities);
    }
}
