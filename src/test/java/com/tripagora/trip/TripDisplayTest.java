package com.tripagora.trip;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 여행 화면에 나오는 상품 정보(제목·소개·사진·지역·태그)와 리뷰의 제목
class TripDisplayTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TEMPLATE = 1L;
    private static final long RECRUITING_TRIP = 1L;
    private static final long COMPLETED_TRIP = 2L;
    private static final String IMAGE_1 = "https://example.com/template1-1.png";
    private static final String IMAGE_2 = "https://example.com/template1-2.png";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LocalDate today = today();
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.region(1L, "서울");
        fixture.region(2L, "부산");
        fixture.tag(1L, "맛집");
        fixture.tag(2L, "역사");
        fixture.template(TEMPLATE, GUIDE, 1);
        fixture.templateImage(TEMPLATE, 1);
        fixture.templateImage(TEMPLATE, 2);
        fixture.templateRegion(TEMPLATE, 1L);
        fixture.templateRegion(TEMPLATE, 2L);
        fixture.templateTag(TEMPLATE, 1L);
        fixture.trip(RECRUITING_TRIP, TEMPLATE, TripStatus.RECRUITING, today.plusDays(10), today.plusDays(10));
        fixture.trip(COMPLETED_TRIP, TEMPLATE, TripStatus.COMPLETED, today.minusDays(10), today.minusDays(10));
        fixture.participation(1L, COMPLETED_TRIP, TRAVELER, Role.TRAVELER);
        fixture.review(1L, TRAVELER, COMPLETED_TRIP, TEMPLATE, GUIDE);
    }

    @Test
    @DisplayName("여행 상세에 제목·소개·사진(순서대로)·지역·태그가 담긴다")
    void tripDetail() throws Exception {
        mockMvc.perform(get("/api/trips/" + RECRUITING_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("플랜1"))
                .andExpect(jsonPath("$.data.content").value("테스트 플랜 소개"))
                .andExpect(jsonPath("$.data.imageUrls", contains(IMAGE_1, IMAGE_2)))
                .andExpect(jsonPath("$.data.regionIds", containsInAnyOrder(1, 2)))
                .andExpect(jsonPath("$.data.tagIds", contains(1)));
    }

    @Test
    @DisplayName("여행 목록에 제목·대표 사진·지역이 담긴다")
    void tripList() throws Exception {
        mockMvc.perform(get("/api/trips/search").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips[0].tripId").value(RECRUITING_TRIP))
                .andExpect(jsonPath("$.data.trips[0].title").value("플랜1"))
                .andExpect(jsonPath("$.data.trips[0].firstImageUrl").value(IMAGE_1))
                .andExpect(jsonPath("$.data.trips[0].regionIds", containsInAnyOrder(1, 2)));
    }

    @Test
    @DisplayName("완료 여행 목록에 제목·대표 사진·지역이 담긴다")
    void completedTripList() throws Exception {
        mockMvc.perform(get("/api/trips/completed").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trips[0].tripId").value(COMPLETED_TRIP))
                .andExpect(jsonPath("$.data.trips[0].title").value("플랜1"))
                .andExpect(jsonPath("$.data.trips[0].firstImageUrl").value(IMAGE_1))
                .andExpect(jsonPath("$.data.trips[0].regionIds", containsInAnyOrder(1, 2)));
    }

    @Test
    @DisplayName("완료 여행만 남은 템플릿의 제목·소개·사진·지역·태그를 고쳐도 지난 여행 화면과 리뷰 제목은 그대로다")
    void completedTripKeepsProductInfoAfterTemplateChanges() throws Exception {
        // 모집 중 여행이 있으면 템플릿을 고칠 수 없으므로 지운다 (완료 여행만 남김)
        mockMvc.perform(delete("/api/trips/" + RECRUITING_TRIP).header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk());

        editTemplate(patch("/api/templates/" + TEMPLATE + "/title"), "{\"title\":\"바뀐 제목\"}");
        editTemplate(patch("/api/templates/" + TEMPLATE + "/content"), "{\"content\":\"바뀐 소개\"}");
        editTemplate(patch("/api/templates/" + TEMPLATE + "/regions"), "{\"regionIds\":[2]}");
        editTemplate(patch("/api/templates/" + TEMPLATE + "/tags"), "{\"tagIds\":[2]}");
        // 사진 교체 (S3 없이 템플릿 사진 행만 바꿔 같은 상황을 만든다)
        jdbcTemplate.update("UPDATE template_image SET image_url = CONCAT(image_url, '?new') WHERE template_id = ?", TEMPLATE);

        mockMvc.perform(get("/api/trips/" + COMPLETED_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(jsonPath("$.data.title").value("플랜1"))
                .andExpect(jsonPath("$.data.content").value("테스트 플랜 소개"))
                .andExpect(jsonPath("$.data.imageUrls", contains(IMAGE_1, IMAGE_2)))
                .andExpect(jsonPath("$.data.regionIds", containsInAnyOrder(1, 2)))
                .andExpect(jsonPath("$.data.tagIds", contains(1)));
        mockMvc.perform(get("/api/trips/completed").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(jsonPath("$.data.trips[0].title").value("플랜1"))
                .andExpect(jsonPath("$.data.trips[0].firstImageUrl").value(IMAGE_1))
                .andExpect(jsonPath("$.data.trips[0].regionIds", containsInAnyOrder(1, 2)));
        mockMvc.perform(get("/api/reviews/guide/" + GUIDE).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(jsonPath("$.data.reviews[0].templateTitle").value("플랜1"));
    }

    @Test
    @DisplayName("리뷰 목록에 리뷰한 여행의 제목이 담긴다")
    void reviewTitle() throws Exception {
        mockMvc.perform(get("/api/reviews/guide/" + GUIDE).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews[0].templateTitle").value("플랜1"));
        mockMvc.perform(get("/api/reviews/template/" + TEMPLATE).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews[0].templateTitle").value("플랜1"));
    }

    private void editTemplate(MockHttpServletRequestBuilder request, String body) throws Exception {
        mockMvc.perform(request.header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
