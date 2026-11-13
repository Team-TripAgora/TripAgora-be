package com.tripagora.template;

import com.tripagora.support.IntegrationTest;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 템플릿 태그·지역 '전체 교체': 요청한 목록만 남는다 (겹치는 항목, 같은 목록 다시 저장, 빈 목록)
class TemplateTagRegionReplaceTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TEMPLATE = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.template(TEMPLATE, GUIDE, 1);
        for (long id = 1; id <= 3; id++) {
            fixture.tag(id, "태그" + id);
            fixture.region(id, "지역" + id);
        }
    }

    @Test
    @DisplayName("태그 [1,2]를 [2,3]으로 바꾸면 2·3만 남는다")
    void replacesOverlappingTags() throws Exception {
        updateTags("[1,2]").andExpect(status().isOk());

        updateTags("[2,3]").andExpect(status().isOk());

        assertThat(savedIds("SELECT tag_id FROM template_tag WHERE template_id = ?")).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("같은 태그 목록을 다시 저장해도 그대로 남는다")
    void savesSameTagsAgain() throws Exception {
        updateTags("[1,2]").andExpect(status().isOk());

        updateTags("[1,2]").andExpect(status().isOk());

        assertThat(savedIds("SELECT tag_id FROM template_tag WHERE template_id = ?")).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("지역 [1,2]를 [2,3]으로 바꾸면 2·3만 남는다")
    void replacesOverlappingRegions() throws Exception {
        updateRegions("[1,2]").andExpect(status().isOk());

        updateRegions("[2,3]").andExpect(status().isOk());

        assertThat(savedIds("SELECT region_id FROM template_region WHERE template_id = ?")).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    @DisplayName("같은 지역 목록을 다시 저장해도 그대로 남는다")
    void savesSameRegionsAgain() throws Exception {
        updateRegions("[1,2]").andExpect(status().isOk());

        updateRegions("[1,2]").andExpect(status().isOk());

        assertThat(savedIds("SELECT region_id FROM template_region WHERE template_id = ?")).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("지역을 빈 목록으로 바꾸면 모두 지워진다")
    void clearsRegionsWithEmptyList() throws Exception {
        updateRegions("[1,2]").andExpect(status().isOk());

        updateRegions("[]").andExpect(status().isOk());

        assertThat(savedIds("SELECT region_id FROM template_region WHERE template_id = ?")).isEmpty();
    }

    private ResultActions updateTags(String tagIds) throws Exception {
        return send("/api/templates/" + TEMPLATE + "/tags", "{\"tagIds\":" + tagIds + "}");
    }

    private ResultActions updateRegions(String regionIds) throws Exception {
        return send("/api/templates/" + TEMPLATE + "/regions", "{\"regionIds\":" + regionIds + "}");
    }

    private ResultActions send(String url, String body) throws Exception {
        return mockMvc.perform(patch(url)
                .header("Authorization", bearer(GUIDE, Role.GUIDE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private List<Long> savedIds(String sql) {
        return jdbcTemplate.queryForList(sql, Long.class, TEMPLATE);
    }
}
