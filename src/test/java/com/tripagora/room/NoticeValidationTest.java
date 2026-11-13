package com.tripagora.room;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 공지 제목·내용은 DB 컬럼(VARCHAR 255)을 넘지 않게 검증한다
class NoticeValidationTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long ROOM = 1L;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();
        fixture.guide(GUIDE);
        fixture.template(1L, GUIDE, 2);
        fixture.trip(1L, 1L, TripStatus.RECRUITMENT_CLOSED, today.plusDays(30), today.plusDays(31));
        fixture.room(ROOM, 1L);
        fixture.participation(1L, 1L, GUIDE, Role.GUIDE);
    }

    @Test
    @DisplayName("공지를 작성한다")
    void createsNotice() throws Exception {
        createNotice("{\"title\":\"집합 안내\",\"content\":\"10시까지 모여주세요\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.noticeId").isNumber());
    }

    @Test
    @DisplayName("공지 내용이 255자를 넘으면 400")
    void rejectsContentOver255Characters() throws Exception {
        createNotice("{\"title\":\"공지\",\"content\":\"" + "a".repeat(256) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].reason").value("내용은 255자 이하여야 합니다."));
    }

    @Test
    @DisplayName("공지 제목을 255자를 넘게 수정하면 400")
    void rejectsUpdatedTitleOver255Characters() throws Exception {
        createNotice("{\"title\":\"집합 안내\",\"content\":\"10시까지 모여주세요\"}").andExpect(status().isOk());

        mockMvc.perform(patch("/api/rooms/" + ROOM + "/notices/1")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "a".repeat(256) + "\",\"content\":\"내용\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].reason").value("제목은 255자 이하여야 합니다."));
    }

    private ResultActions createNotice(String body) throws Exception {
        return mockMvc.perform(post("/api/rooms/" + ROOM + "/notices")
                .header("Authorization", bearer(GUIDE, Role.GUIDE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
