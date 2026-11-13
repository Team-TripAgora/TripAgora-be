package com.tripagora;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 다른 테스트가 부르지 않는 조회 API의 응답. 응답에 연관 엔티티(지연 로딩) 값이 끝까지 채워지는지 확인한다
// OSIV를 끈 뒤에는 서비스 트랜잭션 밖에서 지연 로딩을 하면 오류가 나므로, 그런 코드가 생기면 이 테스트가 잡는다
class ReadApiResponseTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TEMPLATE = 1L;
    private static final long TRIP = 1L;
    private static final long ROOM = 1L;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.region(1L, "서울");
        fixture.tag(1L, "맛집");
        fixture.template(TEMPLATE, GUIDE, 2);
        fixture.templateImage(TEMPLATE, 0);
        fixture.templateRegion(TEMPLATE, 1L);
        fixture.templateTag(TEMPLATE, 1L);
        fixture.trip(TRIP, TEMPLATE, TripStatus.RECRUITMENT_CLOSED, today.plusDays(30), today.plusDays(31));
        fixture.tripItinerary(TRIP, 1);
        fixture.tripItinerary(TRIP, 2);
        fixture.room(ROOM, TRIP);
        fixture.participation(1L, TRIP, GUIDE, Role.GUIDE);
        fixture.participation(2L, TRIP, TRAVELER, Role.TRAVELER);
    }

    @Test
    @DisplayName("템플릿 상세에 지역·태그·이미지가 담긴다")
    void templateDetail() throws Exception {
        mockMvc.perform(get("/api/templates/" + TEMPLATE).header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("플랜1"))
                .andExpect(jsonPath("$.data.regionIds[0]").value(1))
                .andExpect(jsonPath("$.data.tagIds[0]").value(1))
                .andExpect(jsonPath("$.data.imageUrls", hasSize(1)));
    }

    @Test
    @DisplayName("템플릿 일정 목록")
    void templateItineraries() throws Exception {
        mockMvc.perform(get("/api/templates/" + TEMPLATE + "/itineraries").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itineraries", hasSize(2)))
                .andExpect(jsonPath("$.data.itineraries[*].location", containsInAnyOrder("장소1", "장소2")));
    }

    @Test
    @DisplayName("여행 일정 목록")
    void tripItineraries() throws Exception {
        mockMvc.perform(get("/api/trips/" + TRIP + "/itineraries").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itineraries", hasSize(2)))
                .andExpect(jsonPath("$.data.itineraries[*].location", containsInAnyOrder("장소1", "장소2")));
    }

    @Test
    @DisplayName("공지 목록은 최신 공지부터 준다")
    void notices() throws Exception {
        fixture.notice(1L, ROOM);
        fixture.notice(2L, ROOM);

        mockMvc.perform(get("/api/rooms/" + ROOM + "/notices").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notices", hasSize(2)))
                .andExpect(jsonPath("$.data.notices[0].title").value("공지2"));
    }

    @Test
    @DisplayName("공지 상세")
    void noticeDetail() throws Exception {
        fixture.notice(1L, ROOM);

        mockMvc.perform(get("/api/rooms/" + ROOM + "/notices/1").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("공지1"))
                .andExpect(jsonPath("$.data.content").value("10시까지 모여주세요"));
    }

    @Test
    @DisplayName("가이드 리뷰 목록에 템플릿 제목과 작성자가 담긴다")
    void guideReviews() throws Exception {
        LocalDate today = LocalDate.now();
        fixture.template(2L, GUIDE, 1);
        fixture.trip(2L, TEMPLATE, TripStatus.COMPLETED, today.minusDays(10), today.minusDays(9));
        fixture.trip(3L, 2L, TripStatus.COMPLETED, today.minusDays(20), today.minusDays(20));
        fixture.review(1L, TRAVELER, 2L, TEMPLATE, GUIDE);
        fixture.review(2L, TRAVELER, 3L, 2L, GUIDE);

        mockMvc.perform(get("/api/reviews/guide/" + GUIDE).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews", hasSize(2)))
                .andExpect(jsonPath("$.data.reviews[*].templateTitle", containsInAnyOrder("플랜1", "플랜2")))
                .andExpect(jsonPath("$.data.reviews[*].authorNickname", containsInAnyOrder("여행객2", "여행객2")));
    }
}
