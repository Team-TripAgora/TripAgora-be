package com.tripagora.template;

import com.tripagora.support.IntegrationTest;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 템플릿 수정 요청의 입력 검증: 잘못된 입력은 500이 아니라 400과 필드별 사유로 응답한다
class TemplateUpdateValidationTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TEMPLATE = 1L;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.template(TEMPLATE, GUIDE, 1);
        fixture.tag(1L, "맛집");
        fixture.region(1L, "서울");
    }

    @Test
    @DisplayName("태그 목록이 없으면 400")
    void rejectsMissingTagIds() throws Exception {
        send(patch("/api/templates/1/tags"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("tagIds"))
                .andExpect(jsonPath("$.errors[0].reason").value("태그 목록은 필수입니다."));
    }

    @Test
    @DisplayName("지역 목록이 없으면 400")
    void rejectsMissingRegionIds() throws Exception {
        send(patch("/api/templates/1/regions"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("regionIds"));
    }

    @Test
    @DisplayName("지역 목록을 비워 보내면 전부 지운다")
    void acceptsEmptyRegionIds() throws Exception {
        send(patch("/api/templates/1/regions"), "{\"regionIds\":[]}")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일정 목록이 없으면 400")
    void rejectsMissingItineraries() throws Exception {
        send(put("/api/templates/1/itineraries"), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("itineraries"));
    }

    @Test
    @DisplayName("제목 255자는 저장된다")
    void acceptsTitleOf255Characters() throws Exception {
        send(patch("/api/templates/1/title"), "{\"title\":\"" + "a".repeat(255) + "\"}")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("제목이 255자를 넘으면 400")
    void rejectsTitleOver255Characters() throws Exception {
        send(patch("/api/templates/1/title"), "{\"title\":\"" + "a".repeat(256) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].reason").value("제목은 255자 이하여야 합니다."));
    }

    @Test
    @DisplayName("내용이 255자를 넘으면 400")
    void rejectsContentOver255Characters() throws Exception {
        send(patch("/api/templates/1/content"), "{\"content\":\"" + "a".repeat(256) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].reason").value("내용은 255자 이하여야 합니다."));
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request
                .header("Authorization", bearer(GUIDE, Role.GUIDE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
