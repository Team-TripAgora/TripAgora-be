package com.tripagora;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 저장하는 시각(생성·수정 시각, 숨긴 시각)은 서버 시간대와 상관없이 한국 시간이다.
// 시계를 UTC 3월 9일 15:30(= 한국 3월 10일 0:30)에 고정해 확인한다
class KoreanTimestampTest extends IntegrationTest {
    private static final Instant KOREA_00_30 = Instant.parse("2026-03-09T15:30:00Z");
    private static final LocalDateTime KOREAN_NOW = LocalDateTime.of(2026, 3, 10, 0, 30);
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long TRIP = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.template(1L, GUIDE, 1);
    }

    @Test
    @DisplayName("공지 작성 시각(생성·수정 시각)은 한국 시간으로 기록된다")
    void noticeCreatedAtIsKoreanTime() throws Exception {
        LocalDate startDate = today().plusDays(30);
        fixture.trip(TRIP, 1L, TripStatus.RECRUITMENT_CLOSED, startDate, startDate);
        fixture.room(1L, TRIP);
        fixture.participation(1L, TRIP, GUIDE, Role.GUIDE);
        clock.fixAt(KOREA_00_30);

        mockMvc.perform(post("/api/rooms/1/notices")
                        .header("Authorization", bearer(GUIDE, Role.GUIDE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"집합 안내\",\"content\":\"10시까지 모여주세요\"}"))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT created_at FROM notice", LocalDateTime.class)).isEqualTo(KOREAN_NOW);
        assertThat(jdbcTemplate.queryForObject("SELECT modified_at FROM notice", LocalDateTime.class)).isEqualTo(KOREAN_NOW);
    }

    @Test
    @DisplayName("완료된 여행을 내 목록에서 숨긴 시각은 한국 시간으로 기록된다")
    void hiddenAtIsKoreanTime() throws Exception {
        LocalDate startDate = today().minusDays(10);
        fixture.trip(TRIP, 1L, TripStatus.COMPLETED, startDate, startDate);
        fixture.participation(1L, TRIP, TRAVELER, Role.TRAVELER);
        clock.fixAt(KOREA_00_30);

        mockMvc.perform(delete("/api/participation/trips/" + TRIP).header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT hidden_at FROM participation WHERE id = 1", LocalDateTime.class)).isEqualTo(KOREAN_NOW);
    }
}
