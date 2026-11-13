package com.tripagora.auth;

import com.sun.net.httpserver.HttpServer;
import com.tripagora.auth.service.LoginService;
import com.tripagora.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 소셜 로그인 첫 가입: 프로필 사진은 받아 오지 못해도 가입을 막지 않는다 (사진이 없으면 image_url은 빈 문자열)
// 카카오 사용자 정보 API는 MockRestServiceServer로, 프로필 사진은 테스트 안의 HTTP 서버로 대신한다 (외부 네트워크 없음)
class SocialLoginProfileImageTest extends IntegrationTest {
    private static final String KAKAO_USERINFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String SOCIAL_ID = "12345";

    @Autowired
    private LoginService loginService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestTemplate kakaoRestTemplate;
    private ClientHttpRequestFactory originalRequestFactory;
    private MockRestServiceServer kakao;
    private HttpServer imageServer;
    private ExecutorService imageServerThreads;

    @BeforeEach
    void setUp() throws IOException {
        Object loginServiceTarget = AopTestUtils.getUltimateTargetObject(loginService);
        kakaoRestTemplate = (RestTemplate) ReflectionTestUtils.getField(loginServiceTarget, "restTemplate");
        originalRequestFactory = kakaoRestTemplate.getRequestFactory();
        kakao = MockRestServiceServer.bindTo(kakaoRestTemplate).build();

        imageServerThreads = Executors.newCachedThreadPool();
        imageServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        imageServer.setExecutor(imageServerThreads);
        imageServer.createContext("/profile.png", exchange -> respond(exchange, "image/png", new byte[]{1, 2, 3}));
        imageServer.createContext("/not-image", exchange -> respond(exchange, "text/html", "<html></html>".getBytes()));
        imageServer.start();

        when(amazonS3.getUrl(anyString(), anyString())).thenAnswer(invocation ->
                new URL("https://test-bucket.s3.ap-northeast-2.amazonaws.com/" + invocation.getArgument(1)));
    }

    @AfterEach
    void tearDown() {
        kakaoRestTemplate.setRequestFactory(originalRequestFactory);
        imageServer.stop(0);
        imageServerThreads.shutdownNow();
    }

    @Test
    @DisplayName("프로필 사진을 받으면 S3에 올린 주소로 가입한다")
    void signsUpWithUploadedProfileImage() throws Exception {
        kakaoReturnsProfileImage("http://localhost:" + imageServer.getAddress().getPort() + "/profile.png");

        login().andExpect(status().isOk()).andExpect(jsonPath("$.data.isNewUser").value(true));

        assertThat(savedImageUrl()).startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/");
    }

    @Test
    @DisplayName("프로필 사진 주소에 연결할 수 없으면 사진 없이 가입한다")
    void signsUpWithoutImageWhenDownloadFails() throws Exception {
        kakaoReturnsProfileImage("http://localhost:" + closedPort() + "/profile.png");

        login().andExpect(status().isOk()).andExpect(jsonPath("$.data.isNewUser").value(true));

        assertThat(savedImageUrl()).isEmpty();
    }

    @Test
    @DisplayName("프로필 사진 주소가 이미지가 아니면 사진 없이 가입한다")
    void signsUpWithoutImageWhenUrlIsNotImage() throws Exception {
        kakaoReturnsProfileImage("http://localhost:" + imageServer.getAddress().getPort() + "/not-image");

        login().andExpect(status().isOk()).andExpect(jsonPath("$.data.isNewUser").value(true));

        assertThat(savedImageUrl()).isEmpty();
    }

    @Test
    @DisplayName("소셜 계정에 프로필 사진 주소가 없으면 사진 없이 가입한다")
    void signsUpWithoutImageWhenSocialHasNoImage() throws Exception {
        kakaoReturns("{\"id\":" + SOCIAL_ID + ",\"kakao_account\":{\"profile\":{}}}");

        login().andExpect(status().isOk()).andExpect(jsonPath("$.data.isNewUser").value(true));

        assertThat(savedImageUrl()).isEmpty();
    }

    private void kakaoReturnsProfileImage(String profileImageUrl) {
        kakaoReturns("{\"id\":" + SOCIAL_ID + ",\"kakao_account\":{\"profile\":{\"profile_image_url\":\"" + profileImageUrl + "\"}}}");
    }

    private void kakaoReturns(String userInfoJson) {
        kakao.expect(requestTo(KAKAO_USERINFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(userInfoJson, MediaType.APPLICATION_JSON));
    }

    private ResultActions login() throws Exception {
        return mockMvc.perform(post("/api/auth/login/KAKAO")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"socialAccessToken\":\"kakao-access-token\"}"));
    }

    private String savedImageUrl() {
        return jdbcTemplate.queryForObject("SELECT image_url FROM users WHERE social_id = ?", String.class, SOCIAL_ID);
    }

    // 아무도 듣고 있지 않은 포트 (연결 거부)
    private static int closedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
