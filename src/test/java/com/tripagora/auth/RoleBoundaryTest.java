package com.tripagora.auth;

import com.jayway.jsonpath.JsonPath;
import com.tripagora.support.IntegrationTest;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 역할(모드) 경계: 여행객으로 바꾸면 가이드 기능은 막히고, 다시 가이드로 바꾸면 열린다. 조회는 누구나 된다
class RoleBoundaryTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long OTHER_TRAVELER = 2L;
    private static final long TEMPLATE = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.traveler(OTHER_TRAVELER);
        fixture.template(TEMPLATE, GUIDE, 1);
    }

    @Test
    @DisplayName("여행객으로 바꾼 뒤에는 예전 가이드 토큰으로 여행을 열 수 없다")
    void oldGuideTokenCannotCreateTripAfterSwitchingToTraveler() throws Exception {
        String oldGuideToken = bearer(GUIDE, Role.GUIDE);
        switchToTraveler(oldGuideToken);

        createTrip(oldGuideToken)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 액세스 토큰입니다."));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trip", Integer.class)).isZero();
    }

    @Test
    @DisplayName("여행객으로 바꾼 뒤 받은 여행객 토큰으로는 템플릿을 만들거나 고칠 수 없다")
    void travelerTokenCannotCreateOrEditTemplates() throws Exception {
        String travelerToken = switchToTraveler(bearer(GUIDE, Role.GUIDE));

        mockMvc.perform(patch("/api/templates/" + TEMPLATE + "/title")
                        .header("Authorization", travelerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"여행객이 바꾼 제목\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("권한이 없습니다."));
        mockMvc.perform(post("/api/templates").header("Authorization", travelerToken))
                .andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForObject("SELECT title FROM template WHERE id = ?", String.class, TEMPLATE))
                .isEqualTo("플랜" + TEMPLATE);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM template", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("여행객 토큰으로는 가이드 소개를 고칠 수 없다")
    void travelerTokenCannotEditGuideProfile() throws Exception {
        String travelerToken = switchToTraveler(bearer(GUIDE, Role.GUIDE));

        mockMvc.perform(patch("/api/guide-profiles/me/bio")
                        .header("Authorization", travelerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"여행객이 바꾼 소개\"}"))
                .andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForObject("SELECT bio FROM guide_profile WHERE id = ?", String.class, GUIDE))
                .isEqualTo("테스트 가이드");
    }

    @Test
    @DisplayName("여행객도 가이드 프로필은 볼 수 있다 (다른 가이드, 내 예전 가이드 프로필)")
    void travelersCanReadGuideProfiles() throws Exception {
        mockMvc.perform(get("/api/guide-profiles/" + GUIDE).header("Authorization", bearer(OTHER_TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk());

        String travelerToken = switchToTraveler(bearer(GUIDE, Role.GUIDE));
        mockMvc.perform(get("/api/guide-profiles/me").header("Authorization", travelerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("다시 가이드로 바꾸면 새 가이드 토큰으로 여행을 열 수 있다")
    void newGuideTokenWorksAfterSwitchingBack() throws Exception {
        String travelerToken = switchToTraveler(bearer(GUIDE, Role.GUIDE));
        String newGuideToken = accessTokenOf(mockMvc.perform(post("/api/users/me/switch-to-guide").header("Authorization", travelerToken))
                .andExpect(status().isOk()));

        createTrip(newGuideToken).andExpect(status().isOk());
    }

    // 여행객으로 바꾸고 새로 받은 액세스 토큰을 돌려준다
    private String switchToTraveler(String guideToken) throws Exception {
        return accessTokenOf(mockMvc.perform(post("/api/users/me/switch-to-traveler").header("Authorization", guideToken))
                .andExpect(status().isOk()));
    }

    private ResultActions createTrip(String token) throws Exception {
        return mockMvc.perform(post("/api/trips")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"templateId\":" + TEMPLATE + ",\"maxParticipants\":4,\"startDate\":\"" + today().plusDays(30) + "\"}"));
    }

    private static String accessTokenOf(ResultActions result) throws Exception {
        return "Bearer " + JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.accessToken");
    }
}
