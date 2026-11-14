package com.tripagora.user;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.tripagora.auth.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 회원 탈퇴: 진행 중이거나 예정된 여행이 있으면 막고, 탈퇴하면 개인정보만 지운다 (남이 보는 이력은 남긴다)
class WithdrawalTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TEMPLATE = 1L;
    private static final long TRIP = 1L;
    private static final String ACTIVE_TRIP_MESSAGE = "진행 중이거나 예정된 여행이 있어 탈퇴할 수 없습니다.";
    private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";
    private static final long ROOM = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LoginService loginService;

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

    @Test
    @DisplayName("탈퇴하면 개인정보를 지우고, 남이 보는 참가·리뷰·채팅 이력은 남긴다")
    void anonymizesTravelerAndKeepsHistory() throws Exception {
        completedTripWithHistory();
        fixture.tag(1L, "맛집");
        fixture.userTag(TRAVELER, 1L);
        fixture.trip(2L, TEMPLATE, TripStatus.COMPLETED, today.minusDays(30), today.minusDays(29));
        fixture.wishlist(TRAVELER, 2L);
        jdbcTemplate.update("UPDATE users SET fcm_token = 'token-b' WHERE id = ?", TRAVELER);

        withdraw(TRAVELER, Role.TRAVELER).andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForMap("SELECT social_id, nickname, image_url, fcm_token FROM users WHERE id = ?", TRAVELER))
                .containsEntry("social_id", "withdrawn-" + TRAVELER)
                .containsEntry("nickname", null)
                .containsEntry("image_url", "")
                .containsEntry("fcm_token", null);
        assertThat(rowCount("SELECT COUNT(*) FROM user_tag WHERE user_id = " + TRAVELER)).isZero();
        assertThat(rowCount("SELECT COUNT(*) FROM wishlist WHERE user_id = " + TRAVELER)).isZero();
        assertThat(rowCount("SELECT COUNT(*) FROM user_location WHERE user_id = " + TRAVELER)).isZero();
        assertThat(rowCount("SELECT COUNT(*) FROM participation WHERE user_id = " + TRAVELER)).isEqualTo(1);
        assertThat(rowCount("SELECT COUNT(*) FROM review WHERE author_id = " + TRAVELER)).isEqualTo(1);
        assertThat(rowCount("SELECT COUNT(*) FROM chat_message WHERE sender_id = " + TRAVELER)).isEqualTo(1);
    }

    @Test
    @DisplayName("탈퇴한 사용자는 남의 화면(채팅·리뷰·참가자 목록)에 '탈퇴한 사용자'로 보인다")
    void showsWithdrawnUserAsWithdrawn() throws Exception {
        completedTripWithHistory();

        withdraw(TRAVELER, Role.TRAVELER).andExpect(status().isOk());

        mockMvc.perform(get("/api/rooms/" + ROOM + "/messages").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(jsonPath("$.data.messages[0].senderNickname").value(WITHDRAWN_NICKNAME))
                .andExpect(jsonPath("$.data.messages[0].senderImageUrl").value(""));
        mockMvc.perform(get("/api/reviews/template/" + TEMPLATE).header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(jsonPath("$.data.reviews[0].authorNickname").value(WITHDRAWN_NICKNAME));
        mockMvc.perform(get("/api/trips/" + TRIP).header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(jsonPath("$.data.participants[1].userId").value(TRAVELER))
                .andExpect(jsonPath("$.data.participants[1].nickname").value(WITHDRAWN_NICKNAME));
    }

    @Test
    @DisplayName("가이드가 탈퇴해도 템플릿·여행과 다른 여행객의 참가·리뷰는 남고, 가이드 프로필의 개인정보만 지운다")
    void guideWithdrawalKeepsOthersHistory() throws Exception {
        tripWithParticipants(TripStatus.COMPLETED);
        fixture.review(1L, TRAVELER, TRIP, TEMPLATE, GUIDE);
        mockMvc.perform(put("/api/guide-profiles/me/portfolios")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"portfolios\":[{\"type\":\"INSTAGRAM\",\"url\":\"https://instagram.com/guide\"}]}"))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE guide_profile SET image_url = 'https://example.com/guide.png' WHERE id = ?", GUIDE);

        withdraw(GUIDE, Role.GUIDE).andExpect(status().isOk());

        assertThat(rowCount("SELECT COUNT(*) FROM template WHERE id = " + TEMPLATE)).isEqualTo(1);
        assertThat(rowCount("SELECT COUNT(*) FROM trip WHERE id = " + TRIP)).isEqualTo(1);
        assertThat(rowCount("SELECT COUNT(*) FROM participation WHERE user_id = " + TRAVELER)).isEqualTo(1);
        assertThat(rowCount("SELECT COUNT(*) FROM review WHERE author_id = " + TRAVELER)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForMap("SELECT bio, image_url FROM guide_profile WHERE id = ?", GUIDE))
                .containsEntry("bio", null)
                .containsEntry("image_url", null);
        assertThat(rowCount("SELECT COUNT(*) FROM guide_portfolio WHERE guide_profile_id = " + GUIDE)).isZero();
        assertThat(socialIdOf(GUIDE)).isEqualTo("withdrawn-" + GUIDE);
    }

    @Test
    @DisplayName("탈퇴 뒤 같은 소셜 계정으로 로그인하면 새 사용자로 가입된다")
    void signsUpAgainAfterWithdrawal() throws Exception {
        jdbcTemplate.update("UPDATE users SET social_id = '777' WHERE id = ?", TRAVELER);
        withdraw(TRAVELER, Role.TRAVELER).andExpect(status().isOk());

        Object loginServiceTarget = AopTestUtils.getUltimateTargetObject(loginService);
        RestTemplate kakaoRestTemplate = (RestTemplate) ReflectionTestUtils.getField(loginServiceTarget, "restTemplate");
        ClientHttpRequestFactory originalRequestFactory = kakaoRestTemplate.getRequestFactory();
        try {
            MockRestServiceServer.bindTo(kakaoRestTemplate).build()
                    .expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("{\"id\":777,\"kakao_account\":{\"profile\":{}}}", MediaType.APPLICATION_JSON));

            mockMvc.perform(post("/api/auth/login/KAKAO")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"socialAccessToken\":\"kakao-access-token\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.isNewUser").value(true));
        } finally {
            kakaoRestTemplate.setRequestFactory(originalRequestFactory);
        }

        assertThat(rowCount("SELECT COUNT(*) FROM users WHERE social_id = '777' AND id <> " + TRAVELER)).isEqualTo(1);
    }

    // 완료된 여행에 여행객이 참가해 리뷰·채팅·위치를 남긴 상태
    private void completedTripWithHistory() {
        tripWithParticipants(TripStatus.COMPLETED);
        fixture.room(ROOM, TRIP);
        fixture.review(1L, TRAVELER, TRIP, TEMPLATE, GUIDE);
        fixture.chatMessage(ROOM, TRAVELER, Role.TRAVELER);
        fixture.userLocation(TRAVELER, ROOM);
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
