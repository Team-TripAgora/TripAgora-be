package com.tripagora.stomp;

import com.tripagora.stomp.exception.InvalidDestinationException;
import com.tripagora.stomp.interceptor.StompAuthInterceptor;
import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 채팅방 STOMP 주소 형식: 구독 /topic/room/{roomId}/(chat|location)과 내 오류 큐 /user/queue/errors, 발행 /publish/room/{roomId}/(chat|location)
class StompDestinationTest extends IntegrationTest {
    private static final long TRAVELER = 2L;
    private static final long ROOM = 10L;

    @Autowired
    private StompAuthInterceptor stompAuthInterceptor;

    @BeforeEach
    void setUp() {
        LocalDate today = today();
        fixture.guide(1L);
        fixture.traveler(TRAVELER);
        fixture.template(1L, 1L, 2);
        fixture.trip(1L, 1L, TripStatus.IN_PROGRESS, today.minusDays(1), today);
        fixture.room(ROOM, 1L);
        fixture.participation(1L, 1L, TRAVELER, Role.TRAVELER);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/topic/room/10/chat", "/topic/room/10/location", "/user/queue/errors"})
    @DisplayName("형식이 맞는 채팅방 구독과 내 오류 큐 구독은 통과한다")
    void validSubscription(String destination) {
        assertThatCode(() -> send(StompCommand.SUBSCRIBE, destination)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/topic/room/abc/chat", "/topic/room/10/unknown", "/topic/room/", "/topic/room/10", "/topic/room/10/chat/extra", "/topic/other", "/queue/errors", "/user/queue/other"})
    @DisplayName("형식이 틀린 구독 주소는 INVALID_DESTINATION으로 거부한다")
    void invalidSubscription(String destination) {
        assertThatThrownBy(() -> send(StompCommand.SUBSCRIBE, destination)).isInstanceOf(InvalidDestinationException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/publish/room/10/chat", "/publish/room/10/location"})
    @DisplayName("형식이 맞는 채팅방 발행은 통과한다")
    void validPublish(String destination) {
        assertThatCode(() -> send(StompCommand.SEND, destination)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/publish/room/abc/chat", "/publish/room/10/unknown", "/publish/room/", "/publish/other"})
    @DisplayName("형식이 틀린 발행 주소는 INVALID_DESTINATION으로 거부한다")
    void invalidPublish(String destination) {
        assertThatThrownBy(() -> send(StompCommand.SEND, destination)).isInstanceOf(InvalidDestinationException.class);
    }

    private void send(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(TRAVELER, null, List.of()));
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        stompAuthInterceptor.preSend(message, null);
    }
}
