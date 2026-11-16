package com.tripagora.common;

import com.tripagora.auth.util.TokenType;
import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 응답의 HTTP 상태와 본문 code는 같다: 새 데이터를 만드는 POST(여행·템플릿·리뷰·공지 생성, 참가 신청, 찜 추가)는 201, 나머지 성공은 200
class ResponseStatusTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TEMPLATE = 1L;
    private static final long RECRUITING_TRIP = 1L;
    private static final long COMPLETED_TRIP = 2L;
    private static final long ROOM = 10L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LocalDate today = today();
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.template(TEMPLATE, GUIDE, 1);
        fixture.trip(RECRUITING_TRIP, TEMPLATE, TripStatus.RECRUITING, today.plusDays(30), today.plusDays(30));
        fixture.trip(COMPLETED_TRIP, TEMPLATE, TripStatus.COMPLETED, today.minusDays(10), today.minusDays(10));
        fixture.room(ROOM, COMPLETED_TRIP);
        fixture.participation(1L, COMPLETED_TRIP, GUIDE, Role.GUIDE);
        fixture.participation(2L, COMPLETED_TRIP, TRAVELER, Role.TRAVELER);
    }

    @Test
    @DisplayName("새 데이터를 만드는 POST(여행·템플릿·리뷰·공지 생성, 참가 신청, 찜 추가)는 201")
    void creationReturns201() throws Exception {
        expectStatus(mockMvc.perform(post("/api/trips").header("Authorization", bearer(GUIDE, Role.GUIDE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"templateId\":" + TEMPLATE + ",\"maxParticipants\":4,\"startDate\":\"" + today().plusDays(60) + "\"}")), 201);
        expectStatus(mockMvc.perform(post("/api/templates").header("Authorization", bearer(GUIDE, Role.GUIDE))), 201);
        expectStatus(mockMvc.perform(post("/api/reviews").header("Authorization", bearer(TRAVELER, Role.TRAVELER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tripId\":" + COMPLETED_TRIP + ",\"content\":\"좋았어요\",\"rating\":5}")), 201);
        expectStatus(mockMvc.perform(post("/api/rooms/" + ROOM + "/notices").header("Authorization", bearer(GUIDE, Role.GUIDE))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"공지\",\"content\":\"10시에 모여요\"}")), 201);
        expectStatus(mockMvc.perform(post("/api/participation/trips/" + RECRUITING_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER))), 201);
        expectStatus(mockMvc.perform(post("/api/wishlist/" + RECRUITING_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER))), 201);
    }

    @Test
    @DisplayName("토큰 재발급과 로그아웃은 새 데이터를 만드는 것이 아니므로 200")
    void reissueAndLogoutReturn200() throws Exception {
        String refreshToken = jwtUtil.createToken(TRAVELER, Role.TRAVELER.name(), TokenType.REFRESH);
        jdbcTemplate.update("INSERT INTO refresh_token (token, user_id) VALUES (?, ?)", refreshToken, TRAVELER);

        expectStatus(mockMvc.perform(post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}")), 200);
        expectStatus(mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(TRAVELER, Role.TRAVELER))), 200);
    }

    @Test
    @DisplayName("조회는 200, 오류는 그 오류의 상태")
    void readAndErrorKeepTheirStatus() throws Exception {
        expectStatus(mockMvc.perform(get("/api/trips/" + RECRUITING_TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER))), 200);
        expectStatus(mockMvc.perform(get("/api/trips/999").header("Authorization", bearer(TRAVELER, Role.TRAVELER))), 404);
    }

    private void expectStatus(ResultActions result, int status) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(status));
    }
}
