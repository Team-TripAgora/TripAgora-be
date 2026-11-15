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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 리프레시 토큰으로 토큰을 다시 받는다 (재발급할 때마다 리프레시 토큰도 새것으로 바뀐다)
class TokenReissueTest extends IntegrationTest {
    private static final long USER = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        fixture.traveler(USER);
    }

    @Test
    @DisplayName("토큰을 받은 직후(같은 초)에 연달아 재발급해도 매번 새 토큰이 나온다")
    void reissuesRepeatedlyWithinTheSameSecond() throws Exception {
        String refreshToken = jwtUtil.createToken(USER, Role.TRAVELER.name(), false);
        jdbcTemplate.update("INSERT INTO refresh_token (token, user_id) VALUES (?, ?)", refreshToken, USER);

        String firstReissued = refreshTokenOf(reissue(refreshToken).andExpect(status().isOk()));
        String secondReissued = refreshTokenOf(reissue(firstReissued).andExpect(status().isOk()));

        assertThat(firstReissued).isNotEqualTo(refreshToken);
        assertThat(secondReissued).isNotEqualTo(firstReissued);
        assertThat(jdbcTemplate.queryForObject("SELECT token FROM refresh_token WHERE user_id = ?", String.class, USER))
                .isEqualTo(secondReissued);
    }

    private ResultActions reissue(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private static String refreshTokenOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.refreshToken");
    }
}
