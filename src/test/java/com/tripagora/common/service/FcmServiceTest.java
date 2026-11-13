package com.tripagora.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

// Firebase로 나가는 HTTP 요청을 가로채 실제로 보내는 메시지 본문을 확인한다. 네트워크로는 나가지 않는다
class FcmServiceTest {
    private final List<String> sentBodies = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FcmService fcmService = new FcmService();

    @BeforeEach
    void initFirebase() {
        FirebaseApp.initializeApp(FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.create(new AccessToken("test-token", new Date(System.currentTimeMillis() + 3_600_000))))
                .setProjectId("test-project")
                .setHttpTransport(new CapturingTransport())
                .build());
    }

    @AfterEach
    void deleteFirebase() {
        FirebaseApp.getInstance().delete();
    }

    @Test
    @DisplayName("SOS 알림은 안드로이드 채널 default 와 여행 id 를 담아 보낸다")
    void buildsSosMessage() throws Exception {
        fcmService.sendMulticastMessage(List.of("token-ok"), "SOS 요청", "도움이 필요합니다", 2L);

        assertThat(sentBodies).hasSize(1);
        JsonNode message = objectMapper.readTree(sentBodies.get(0)).path("message");
        assertThat(message.path("token").asText()).isEqualTo("token-ok");
        assertThat(message.path("notification").path("title").asText()).isEqualTo("SOS 요청");
        assertThat(message.path("notification").path("body").asText()).isEqualTo("도움이 필요합니다");
        assertThat(message.path("android").path("notification").path("channel_id").asText()).isEqualTo("default");
        assertThat(message.path("data").path("type").asText()).isEqualTo("SOS");
        assertThat(message.path("data").path("tripId").asText()).isEqualTo("2");
    }

    @Test
    @DisplayName("토큰마다 한 번씩 보내고, 일부가 실패해도 예외를 던지지 않는다")
    void sendsPerTokenAndToleratesPartialFailure() {
        assertThatCode(() -> fcmService.sendMulticastMessage(List.of("token-ok", "token-gone"), "SOS 요청", "도움이 필요합니다", 2L))
                .doesNotThrowAnyException();

        assertThat(sentBodies).hasSize(2);
    }

    @Test
    @DisplayName("토큰이 없으면 보내지 않는다")
    void sendsNothingWithoutTokens() {
        fcmService.sendMulticastMessage(List.of(), "SOS 요청", "도움이 필요합니다", 2L);

        assertThat(sentBodies).isEmpty();
    }

    // 요청 본문을 모아 두고, "token-gone" 이면 만료된 토큰(UNREGISTERED) 응답을, 아니면 성공 응답을 준다
    private class CapturingTransport extends HttpTransport {
        @Override
        protected LowLevelHttpRequest buildRequest(String method, String url) {
            return new LowLevelHttpRequest() {
                @Override
                public void addHeader(String name, String value) {
                }

                @Override
                public LowLevelHttpResponse execute() throws IOException {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    if (getStreamingContent() != null) {
                        getStreamingContent().writeTo(out);
                    }
                    String body = out.toString(StandardCharsets.UTF_8);
                    sentBodies.add(body);
                    if (body.contains("token-gone")) {
                        return new FakeResponse(404, "{\"error\":{\"code\":404,\"message\":\"Requested entity was not found.\","
                                + "\"status\":\"NOT_FOUND\",\"details\":[{\"@type\":\"type.googleapis.com/google.firebase.fcm.v1.FcmError\","
                                + "\"errorCode\":\"UNREGISTERED\"}]}}");
                    }
                    return new FakeResponse(200, "{\"name\":\"projects/test-project/messages/1\"}");
                }
            };
        }
    }

    private static class FakeResponse extends LowLevelHttpResponse {
        private final int status;
        private final byte[] body;

        FakeResponse(int status, String body) {
            this.status = status;
            this.body = body.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public InputStream getContent() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public String getContentEncoding() {
            return null;
        }

        @Override
        public long getContentLength() {
            return body.length;
        }

        @Override
        public String getContentType() {
            return "application/json; charset=UTF-8";
        }

        @Override
        public String getStatusLine() {
            return "HTTP/1.1 " + status;
        }

        @Override
        public int getStatusCode() {
            return status;
        }

        @Override
        public String getReasonPhrase() {
            return status == 200 ? "OK" : "Not Found";
        }

        @Override
        public int getHeaderCount() {
            return 0;
        }

        @Override
        public String getHeaderName(int index) {
            return null;
        }

        @Override
        public String getHeaderValue(int index) {
            return null;
        }
    }
}
