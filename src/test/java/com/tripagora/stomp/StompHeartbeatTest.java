package com.tripagora.stomp;

import com.tripagora.support.IntegrationTest;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 서버를 띄우고 STOMP 클라이언트로 접속해, 서버가 하트비트 요청에 응하는지 CONNECTED 프레임으로 확인한다
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StompHeartbeatTest extends IntegrationTest {
    private static final long[] APP_HEARTBEAT = {4000, 4000};

    @LocalServerPort
    private int port;

    private ThreadPoolTaskScheduler clientScheduler;
    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUpClient() {
        clientScheduler = new ThreadPoolTaskScheduler();
        clientScheduler.initialize();
        stompClient = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        stompClient.setTaskScheduler(clientScheduler);
        stompClient.setDefaultHeartbeat(APP_HEARTBEAT);
    }

    @AfterEach
    void tearDownClient() {
        stompClient.stop();
        clientScheduler.shutdown();
    }

    @Test
    @DisplayName("앱이 하트비트(4초)를 요청하면 서버가 10초 간격 하트비트로 응답한다")
    void negotiatesHeartbeat() throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", bearer(1L, Role.TRAVELER));
        CompletableFuture<long[]> serverHeartbeat = new CompletableFuture<>();

        StompSession session = stompClient.connectAsync("http://localhost:" + port + "/connect", (WebSocketHttpHeaders) null,
                connectHeaders, new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        serverHeartbeat.complete(connectedHeaders.getHeartbeat());
                    }
                }).get(10, TimeUnit.SECONDS);

        assertThat(serverHeartbeat.get(5, TimeUnit.SECONDS)).containsExactly(10000, 10000);
        session.disconnect();
    }
}
