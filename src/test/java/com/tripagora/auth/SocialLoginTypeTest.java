package com.tripagora.auth;

import com.tripagora.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 소셜 로그인은 카카오만 받는다. 다른 소셜 타입은 외부 API를 부르기 전에 400으로 거절한다
class SocialLoginTypeTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("카카오가 아닌 소셜 타입으로 로그인하면 400이고 사용자를 만들지 않는다")
    void rejectsUnsupportedSocialType() throws Exception {
        mockMvc.perform(post("/api/auth/login/NAVER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"socialAccessToken\":\"naver-access-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
    }
}
