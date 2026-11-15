package com.tripagora.auth;

import com.tripagora.support.IntegrationTest;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 인증 필터: 어떤 토큰이 통과하고 어떤 토큰이 막히는가
class AuthFilterTest extends IntegrationTest {
    private static final long USER = 1L;
    private static final String INVALID_TOKEN_MESSAGE = "유효하지 않은 액세스 토큰입니다.";

    @BeforeEach
    void setUp() {
        fixture.traveler(USER);
    }

    @Test
    @DisplayName("유효한 액세스 토큰이면 통과한다")
    void acceptsValidAccessToken() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(USER, Role.TRAVELER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("토큰이 없으면 401")
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("인증되지 않은 접근입니다."));
    }

    @Test
    @DisplayName("리프레시 토큰으로는 API를 호출할 수 없다")
    void rejectsRefreshTokenAsAccessToken() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + refreshToken(USER)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(INVALID_TOKEN_MESSAGE));
    }

    @Test
    @DisplayName("형식이 틀린 토큰이면 401")
    void rejectsMalformedToken() throws Exception {
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(INVALID_TOKEN_MESSAGE));
    }

    private String refreshToken(long userId) {
        return jwtUtil.createToken(userId, Role.TRAVELER.name(), false);
    }
}
