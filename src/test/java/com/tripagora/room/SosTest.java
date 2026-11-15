package com.tripagora.room;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SOS는 진행 중인 여행의 참가자만 보낼 수 있고, 같은 여행의 다른 참가자에게 간다
class SosTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long OTHER_TRAVELER = 3L;
    private static final long OUTSIDER = 4L;
    private static final long TEMPLATE = 1L;
    private static final long IN_PROGRESS_TRIP = 1L;
    private static final long CLOSED_TRIP = 2L;
    private static final long IN_PROGRESS_ROOM = 10L;
    private static final long CLOSED_ROOM = 20L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LocalDate today = today();
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.traveler(OTHER_TRAVELER);
        fixture.traveler(OUTSIDER);
        fixture.template(TEMPLATE, GUIDE, 2);
        fixture.trip(IN_PROGRESS_TRIP, TEMPLATE, TripStatus.IN_PROGRESS, today.minusDays(1), today);
        fixture.trip(CLOSED_TRIP, TEMPLATE, TripStatus.RECRUITMENT_CLOSED, today.plusDays(10), today.plusDays(11));
        fixture.room(IN_PROGRESS_ROOM, IN_PROGRESS_TRIP);
        fixture.room(CLOSED_ROOM, CLOSED_TRIP);
        fixture.participation(1L, IN_PROGRESS_TRIP, GUIDE, Role.GUIDE);
        fixture.participation(2L, IN_PROGRESS_TRIP, TRAVELER, Role.TRAVELER);
        fixture.participation(3L, IN_PROGRESS_TRIP, OTHER_TRAVELER, Role.TRAVELER);
        fixture.participation(4L, CLOSED_TRIP, TRAVELER, Role.TRAVELER);
        fixture.participation(5L, CLOSED_TRIP, OTHER_TRAVELER, Role.TRAVELER);
        for (long userId = GUIDE; userId <= OUTSIDER; userId++) {
            jdbcTemplate.update("UPDATE users SET fcm_token = ? WHERE id = ?", "token-" + userId, userId);
        }
    }

    @Test
    @DisplayName("진행 중인 여행의 참가자가 보내면 보낸 사람을 뺀 같은 여행 참가자에게 간다")
    @SuppressWarnings("unchecked")
    void participantOfInProgressTripSendsSos() throws Exception {
        sendSos(TRAVELER, IN_PROGRESS_ROOM).andExpect(status().isOk());

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.forClass(List.class);
        verify(fcmService).sendMulticastMessage(tokens.capture(), anyString(), anyString(), eq(IN_PROGRESS_TRIP));
        assertThat(tokens.getValue()).containsExactlyInAnyOrder("token-1", "token-3");
    }

    @Test
    @DisplayName("그 여행의 참가자가 아니면 403이고 SOS가 가지 않는다")
    void outsiderCannotSendSos() throws Exception {
        sendSos(OUTSIDER, IN_PROGRESS_ROOM)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("권한이 없습니다."));

        verify(fcmService, never()).sendMulticastMessage(anyList(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("출발 전(모집 마감) 여행에서는 400이고 SOS가 가지 않는다")
    void sosBeforeTripStartsIsRejected() throws Exception {
        sendSos(TRAVELER, CLOSED_ROOM)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("진행중인 여행에만 가능한 요청입니다."));

        verify(fcmService, never()).sendMulticastMessage(anyList(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("FCM 발송은 DB 트랜잭션 밖에서 한다 (외부 호출 동안 DB 커넥션을 잡지 않는다)")
    void sendsFcmOutsideTransaction() throws Exception {
        AtomicBoolean inTransaction = new AtomicBoolean(true);
        doAnswer(invocation -> {
            inTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(fcmService).sendMulticastMessage(anyList(), anyString(), anyString(), any());

        sendSos(TRAVELER, IN_PROGRESS_ROOM).andExpect(status().isOk());

        verify(fcmService).sendMulticastMessage(anyList(), anyString(), anyString(), eq(IN_PROGRESS_TRIP));
        assertThat(inTransaction).isFalse();
    }

    private ResultActions sendSos(long userId, long roomId) throws Exception {
        return mockMvc.perform(post("/api/rooms/" + roomId + "/sos").header("Authorization", bearer(userId, Role.TRAVELER)));
    }
}
