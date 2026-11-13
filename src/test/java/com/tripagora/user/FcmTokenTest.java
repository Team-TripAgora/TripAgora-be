package com.tripagora.user;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// FCM 토큰 등록 (SOS 알림을 받을 기기)
class FcmTokenTest extends IntegrationTest {
    private static final long USER_A = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        fixture.traveler(USER_A);
    }

    @Test
    @DisplayName("FCM 토큰을 등록한다")
    void registersToken() throws Exception {
        registerToken(USER_A, "token-a").andExpect(status().isOk());

        assertThat(savedToken(USER_A)).isEqualTo("token-a");
    }

    @Test
    @DisplayName("FCM 토큰이 비어 있으면 400")
    void rejectsBlankToken() throws Exception {
        registerToken(USER_A, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].reason").value("FCM 토큰은 필수입니다"));
    }

    @Test
    @DisplayName("로그아웃하면 FCM 토큰이 지워진다 (로그아웃한 기기로 SOS가 가지 않는다)")
    void clearsTokenOnLogout() throws Exception {
        registerToken(USER_A, "token-a").andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(USER_A, Role.TRAVELER)))
                .andExpect(status().isOk());

        assertThat(savedToken(USER_A)).isNull();
    }

    private ResultActions registerToken(long userId, String token) throws Exception {
        return mockMvc.perform(post("/api/users/me/fcm-token")
                .header("Authorization", bearer(userId, Role.TRAVELER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\"}"));
    }

    private String savedToken(long userId) {
        return jdbcTemplate.queryForObject("SELECT fcm_token FROM users WHERE id = ?", String.class, userId);
    }
}
