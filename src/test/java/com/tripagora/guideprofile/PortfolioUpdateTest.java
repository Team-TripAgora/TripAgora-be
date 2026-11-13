package com.tripagora.guideprofile;

import com.tripagora.support.IntegrationTest;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 가이드 포트폴리오 '전체 교체'
class PortfolioUpdateTest extends IntegrationTest {
    private static final long GUIDE = 1L;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
    }

    @Test
    @DisplayName("포트폴리오를 새 목록으로 바꾼다")
    void replacesPortfolios() throws Exception {
        updatePortfolios("{\"portfolios\":[{\"type\":\"INSTAGRAM\",\"url\":\"https://instagram.com/old\"}]}")
                .andExpect(status().isOk());

        updatePortfolios("{\"portfolios\":[{\"type\":\"YOUTUBE\",\"url\":\"https://youtube.com/new\"},"
                + "{\"type\":\"WEBSITE\",\"url\":\"https://example.com\"}]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.portfolios[*].type", contains("YOUTUBE", "WEBSITE")));

        mockMvc.perform(get("/api/guide-profiles/me").header("Authorization", bearer(GUIDE, Role.GUIDE)))
                .andExpect(jsonPath("$.data.portfolios[*].url", contains("https://youtube.com/new", "https://example.com")));
    }

    @Test
    @DisplayName("포트폴리오 목록을 보내지 않으면 모두 지운다")
    void clearsPortfoliosWithoutList() throws Exception {
        updatePortfolios("{\"portfolios\":[{\"type\":\"INSTAGRAM\",\"url\":\"https://instagram.com/old\"}]}")
                .andExpect(status().isOk());

        updatePortfolios("{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.portfolios", empty()));
    }

    private ResultActions updatePortfolios(String body) throws Exception {
        return mockMvc.perform(put("/api/guide-profiles/me/portfolios")
                .header("Authorization", bearer(GUIDE, Role.GUIDE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
