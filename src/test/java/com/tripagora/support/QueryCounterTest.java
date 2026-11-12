package com.tripagora.support;

import com.tripagora.user.entity.Role;
import com.tripagora.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QueryCounterTest extends IntegrationTest {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("쿼리 하나를 정확히 센다")
    void countsSingleQuery() {
        queryCounter.clear();

        userRepository.count();

        assertThat(queryCounter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("clear 하면 0부터 다시 센다")
    void clearResetsCount() {
        userRepository.count();

        queryCounter.clear();

        assertThat(queryCounter.count()).isZero();
    }

    @Test
    @DisplayName("API 한 번 호출에 실행된 쿼리 수를 잰다")
    void countsQueriesOfApiCall() throws Exception {
        jdbcTemplate.update("INSERT INTO users (id, created_at, modified_at, image_url, nickname, role, social_id, social_type) " +
                "VALUES (1, NOW(6), NOW(6), 'https://example.com/u.png', '사용자', 'TRAVELER', 'social-1', 'KAKAO')");
        queryCounter.clear();

        mockMvc.perform(get("/api/users/me").header("Authorization", bearer(1L, Role.TRAVELER)))
                .andExpect(status().isOk());

        assertThat(queryCounter.count()).isPositive();
    }
}
