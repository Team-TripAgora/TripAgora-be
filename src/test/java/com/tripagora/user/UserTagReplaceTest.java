package com.tripagora.user;

import com.tripagora.support.IntegrationTest;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 사용자 관심 태그 '전체 교체': 요청한 목록만 남는다
class UserTagReplaceTest extends IntegrationTest {
    private static final long USER = 1L;

    @BeforeEach
    void setUp() {
        fixture.traveler(USER);
        for (long id = 1; id <= 4; id++) {
            fixture.tag(id, "태그" + id);
        }
    }

    @Test
    @DisplayName("태그 [1,2,3]을 [2,3,4]로 바꾸면 2·3·4만 남는다")
    void replacesOverlappingTags() throws Exception {
        updateTags("[1,2,3]").andExpect(status().isOk());

        updateTags("[2,3,4]").andExpect(status().isOk());

        myTags().andExpect(jsonPath("$.data.tagIds", containsInAnyOrder(2, 3, 4)));
    }

    @Test
    @DisplayName("같은 태그 목록을 다시 저장해도 그대로 남는다")
    void savesSameTagsAgain() throws Exception {
        updateTags("[1,2,3]").andExpect(status().isOk());

        updateTags("[1,2,3]").andExpect(status().isOk());

        myTags().andExpect(jsonPath("$.data.tagIds", containsInAnyOrder(1, 2, 3)));
    }

    private ResultActions updateTags(String tagIds) throws Exception {
        return mockMvc.perform(patch("/api/users/me/tags")
                .header("Authorization", bearer(USER, Role.TRAVELER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tagIds\":" + tagIds + "}"));
    }

    private ResultActions myTags() throws Exception {
        return mockMvc.perform(get("/api/users/me").header("Authorization", bearer(USER, Role.TRAVELER)))
                .andExpect(status().isOk());
    }
}
