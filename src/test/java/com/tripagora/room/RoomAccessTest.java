package com.tripagora.room;

import com.tripagora.auth.exception.AccessDeniedException;
import com.tripagora.room.exception.RoomNotFoundException;
import com.tripagora.stomp.interceptor.StompAuthInterceptor;
import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 채팅방(채팅 이력·위치·공지·구독)은 그 여행의 참가자만 들어올 수 있다. 참가자가 아니면 403, 채팅방이 없으면 404
class RoomAccessTest extends IntegrationTest {
    private static final long GUIDE = 1L;
    private static final long TRAVELER = 2L;
    private static final long OUTSIDER = 3L;
    private static final long TEMPLATE = 1L;
    private static final long TRIP = 1L;
    private static final long ROOM = 10L;
    private static final long NOTICE = 1L;
    private static final long MISSING_ROOM = 999L;

    @Autowired
    private StompAuthInterceptor stompAuthInterceptor;

    @BeforeEach
    void setUp() {
        LocalDate today = today();
        fixture.guide(GUIDE);
        fixture.traveler(TRAVELER);
        fixture.traveler(OUTSIDER);
        fixture.template(TEMPLATE, GUIDE, 2);
        fixture.trip(TRIP, TEMPLATE, TripStatus.IN_PROGRESS, today.minusDays(1), today);
        fixture.room(ROOM, TRIP);
        fixture.participation(1L, TRIP, GUIDE, Role.GUIDE);
        fixture.participation(2L, TRIP, TRAVELER, Role.TRAVELER);
        fixture.notice(NOTICE, ROOM);
    }

    @Test
    @DisplayName("참가자는 채팅 이력·위치·공지 목록·공지 상세를 볼 수 있다")
    void participantCanReadRoom() throws Exception {
        readRoom(TRAVELER, "/messages").andExpect(status().isOk());
        readRoom(TRAVELER, "/locations").andExpect(status().isOk());
        readRoom(TRAVELER, "/notices").andExpect(status().isOk());
        readRoom(TRAVELER, "/notices/" + NOTICE).andExpect(status().isOk());
    }

    @Test
    @DisplayName("참가자가 아니면 채팅 이력·위치·공지 목록·공지 상세가 403이다")
    void outsiderIsForbidden() throws Exception {
        for (String path : List.of("/messages", "/locations", "/notices", "/notices/" + NOTICE)) {
            readRoom(OUTSIDER, path)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("권한이 없습니다."));
        }
    }

    @Test
    @DisplayName("없는 채팅방은 404다")
    void missingRoomIsNotFound() throws Exception {
        mockMvc.perform(get("/api/rooms/" + MISSING_ROOM + "/messages").header("Authorization", bearer(TRAVELER, Role.TRAVELER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("방을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("가이드가 아닌 참가자는 공지를 쓸 수 없다")
    void travelerCannotWriteNotice() throws Exception {
        mockMvc.perform(post("/api/rooms/" + ROOM + "/notices")
                        .header("Authorization", bearer(TRAVELER, Role.TRAVELER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"공지\",\"content\":\"내용\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("참가자는 채팅방을 구독할 수 있고, 참가자가 아니면 권한 없음으로 거부된다")
    void subscribeChecksParticipation() {
        assertThatCode(() -> subscribe(TRAVELER, ROOM)).doesNotThrowAnyException();
        assertThatThrownBy(() -> subscribe(OUTSIDER, ROOM)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> subscribe(TRAVELER, MISSING_ROOM)).isInstanceOf(RoomNotFoundException.class);
    }

    private ResultActions readRoom(long userId, String path) throws Exception {
        return mockMvc.perform(get("/api/rooms/" + ROOM + path).header("Authorization", bearer(userId, Role.TRAVELER)));
    }

    private void subscribe(long userId, long roomId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/room/" + roomId + "/chat");
        accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        stompAuthInterceptor.preSend(message, null);
    }
}
