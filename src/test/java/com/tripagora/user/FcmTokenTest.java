package com.tripagora.user;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// FCM 토큰 등록 (SOS 알림을 받을 기기)
class FcmTokenTest extends IntegrationTest {
    private static final long USER_A = 1L;
    private static final long USER_B = 2L;

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

    @Test
    @DisplayName("같은 FCM 토큰을 다른 계정이 등록하면 이전 계정에서 지워진다 (같은 기기에서 계정을 바꾼 경우)")
    void movesTokenToLatestAccount() throws Exception {
        fixture.traveler(USER_B);
        registerToken(USER_A, "shared-device").andExpect(status().isOk());

        registerToken(USER_B, "shared-device").andExpect(status().isOk());

        assertThat(savedToken(USER_A)).isNull();
        assertThat(savedToken(USER_B)).isEqualTo("shared-device");
    }

    @Test
    @DisplayName("계정을 바꾼 기기로는 이전 계정 앞으로 온 SOS가 가지 않는다")
    @SuppressWarnings("unchecked")
    void sosDoesNotReachDeviceOfSwitchedAccount() throws Exception {
        // A(1)·C(3)·D(4)가 같은 여행에 참가. B(2)는 참가하지 않았고, A가 쓰던 기기로 로그인한다
        LocalDate today = LocalDate.now();
        fixture.traveler(USER_B);
        fixture.traveler(3L);
        fixture.traveler(4L);
        fixture.guide(10L);
        fixture.template(1L, 10L, 2);
        fixture.trip(1L, 1L, TripStatus.IN_PROGRESS, today.minusDays(1), today);
        fixture.room(1L, 1L);
        fixture.participation(1L, 1L, USER_A, Role.TRAVELER);
        fixture.participation(3L, 1L, 3L, Role.TRAVELER);
        fixture.participation(4L, 1L, 4L, Role.TRAVELER);
        registerToken(USER_A, "shared-device").andExpect(status().isOk());
        registerToken(4L, "token-d").andExpect(status().isOk());
        registerToken(USER_B, "shared-device").andExpect(status().isOk());

        mockMvc.perform(post("/api/rooms/1/sos").header("Authorization", bearer(3L, Role.TRAVELER)))
                .andExpect(status().isOk());

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.forClass(List.class);
        verify(fcmService).sendMulticastMessage(tokens.capture(), anyString(), anyString(), eq(1L));
        assertThat(tokens.getValue()).containsExactly("token-d");
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
