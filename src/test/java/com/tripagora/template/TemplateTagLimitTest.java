package com.tripagora.template;

import com.tripagora.support.IntegrationTest;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 템플릿 태그는 최대 5개다 (요청 검증 한 곳에서 막는다)
class TemplateTagLimitTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TEMPLATE = 1L;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.template(TEMPLATE, GUIDE, 1);
        for (long tagId = 1; tagId <= 6; tagId++) {
            fixture.tag(tagId, "태그" + tagId);
        }
    }

    @Test
    @DisplayName("템플릿 태그를 6개 고르면 400이고, 태그는 5개까지라고 알려 준다")
    void rejectsMoreThanFiveTags() throws Exception {
        mockMvc.perform(patch("/api/templates/" + TEMPLATE + "/tags")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[1,2,3,4,5,6]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("tagIds"))
                .andExpect(jsonPath("$.errors[0].reason").value("최대 5개까지 태그를 선택할 수 있습니다."));
    }

    @Test
    @DisplayName("템플릿 태그는 5개까지 고를 수 있다")
    void acceptsFiveTags() throws Exception {
        mockMvc.perform(patch("/api/templates/" + TEMPLATE + "/tags")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[1,2,3,4,5]}"))
                .andExpect(status().isOk());
    }
}
