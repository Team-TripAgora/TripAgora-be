package com.tripagora.stomp;

import com.tripagora.support.IntegrationTest;
import com.tripagora.trip.entity.TripStatus;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 채팅·위치 메시지(SEND) 처리 중 오류는 보낸 사람에게만 /user/queue/errors로 오류 응답(코드·메시지)이 간다
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StompMessageErrorTest extends IntegrationTest {
    private static final long TRAVELER = 2L;
    private static final long OUTSIDER = 3L;
    private static final long ROOM = 10L;

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ThreadPoolTaskScheduler clientScheduler;
    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        LocalDate today = today();
        fixture.guide(1L);
        fixture.traveler(TRAVELER);
        fixture.traveler(OUTSIDER);
        fixture.template(1L, 1L, 2);
        fixture.trip(1L, 1L, TripStatus.IN_PROGRESS, today.minusDays(1), today);
        fixture.room(ROOM, 1L);
        fixture.participation(1L, 1L, TRAVELER, Role.TRAVELER);

        clientScheduler = new ThreadPoolTaskScheduler();
        clientScheduler.initialize();
        stompClient = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setTaskScheduler(clientScheduler);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        stompClient.stop();
        clientScheduler.shutdown();
    }

    @Test
    @DisplayName("빈 채팅을 보내면 보낸 사람이 400 오류와 이유를 받고, 메시지는 저장되지 않는다")
    void blankChatIsRejectedToSender() throws Exception {
        StompSession session = connect(TRAVELER);
        BlockingQueue<Map<String, Object>> errors = subscribe(session, "/user/queue/errors");

        Map<String, Object> error = sendUntilReceived(session, "/publish/room/" + ROOM + "/chat", Map.of("content", " "), errors);

        assertThat(error).containsEntry("code", 400);
        assertThat((List<Map<String, Object>>) error.get("errors"))
                .extracting(fieldError -> fieldError.get("reason"))
                .containsExactly("메시지 내용은 필수입니다.");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_message", Integer.class)).isZero();
        session.disconnect();
    }

    @Test
    @DisplayName("위도·경도가 범위를 벗어나면 보낸 사람이 400 오류를 받는다")
    void outOfRangeLocationIsRejectedToSender() throws Exception {
        StompSession session = connect(TRAVELER);
        BlockingQueue<Map<String, Object>> errors = subscribe(session, "/user/queue/errors");

        Map<String, Object> error = sendUntilReceived(session, "/publish/room/" + ROOM + "/location",
                Map.of("latitude", 91.0, "longitude", 127.0), errors);

        assertThat(error).containsEntry("code", 400);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_location", Integer.class)).isZero();
        session.disconnect();
    }

    @Test
    @DisplayName("참가자가 아닌 사람이 채팅을 보내면 그 사람이 403 오류를 받고, 메시지는 저장되지 않는다")
    void outsiderChatIsRejectedToSender() throws Exception {
        StompSession session = connect(OUTSIDER);
        BlockingQueue<Map<String, Object>> errors = subscribe(session, "/user/queue/errors");

        Map<String, Object> error = sendUntilReceived(session, "/publish/room/" + ROOM + "/chat", Map.of("content", "안녕하세요"), errors);

        assertThat(error).containsEntry("code", 403).containsEntry("message", "권한이 없습니다.");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chat_message", Integer.class)).isZero();
        session.disconnect();
    }

    @Test
    @DisplayName("참가자가 보낸 정상 채팅은 채팅방 구독자에게 전달된다")
    void participantChatIsBroadcast() throws Exception {
        StompSession session = connect(TRAVELER);
        BlockingQueue<Map<String, Object>> chats = subscribe(session, "/topic/room/" + ROOM + "/chat");

        Map<String, Object> chat = sendUntilReceived(session, "/publish/room/" + ROOM + "/chat", Map.of("content", "안녕하세요"), chats);

        assertThat(chat).containsEntry("content", "안녕하세요").containsEntry("senderId", (int) TRAVELER);
        session.disconnect();
    }

    private StompSession connect(long userId) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", bearer(userId, Role.TRAVELER));
        return stompClient.connectAsync("http://localhost:" + port + "/connect", (WebSocketHttpHeaders) null,
                connectHeaders, new StompSessionHandlerAdapter() {
                }).get(10, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((Map<String, Object>) payload);
            }
        });
        return received;
    }

    // 구독 처리가 끝나기 전에 보낸 메시지는 받지 못할 수 있어, 받을 때까지 0.2초 간격으로 다시 보낸다 (최대 5초)
    private Map<String, Object> sendUntilReceived(StompSession session, String destination, Object payload,
                                                  BlockingQueue<Map<String, Object>> received) throws InterruptedException {
        for (int attempt = 0; attempt < 25; attempt++) {
            session.send(destination, payload);
            Map<String, Object> message = received.poll(200, TimeUnit.MILLISECONDS);
            if (message != null) {
                return message;
            }
        }
        throw new AssertionError("5초 안에 메시지를 받지 못했습니다: " + destination);
    }
}
