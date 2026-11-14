package com.tripagora.auth;

import com.amazonaws.services.s3.model.PutObjectRequest;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 소셜 로그인: 외부 호출(카카오 API, 프로필 사진 업로드)은 DB 트랜잭션 밖에서 하고, 사용자·토큰 저장만 트랜잭션으로 한다
class SocialLoginTransactionTest extends IntegrationTest {
    private static final String KAKAO_USERINFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String SOCIAL_ID = "12345";
    private static final String BUCKET = "test-bucket";
    private static final String S3_URL = "https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/";

    @Autowired
    private LoginService loginService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RestTemplate kakaoRestTemplate;
    private ClientHttpRequestFactory originalRequestFactory;
    private MockRestServiceServer kakao;
    private HttpServer imageServer;
    private ExecutorService imageServerThreads;

    // 외부 호출 순간 DB 트랜잭션이 열려 있었는지, 올린 파일의 키
    private final List<Boolean> transactionActiveWhileCallingKakao = new CopyOnWriteArrayList<>();
    private final List<Boolean> transactionActiveWhileUploading = new CopyOnWriteArrayList<>();
    private final List<String> uploadedKeys = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        Object loginServiceTarget = AopTestUtils.getUltimateTargetObject(loginService);
        kakaoRestTemplate = (RestTemplate) ReflectionTestUtils.getField(loginServiceTarget, "restTemplate");
        originalRequestFactory = kakaoRestTemplate.getRequestFactory();
        kakao = MockRestServiceServer.bindTo(kakaoRestTemplate).build();

        imageServerThreads = Executors.newCachedThreadPool();
        imageServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        imageServer.setExecutor(imageServerThreads);
        imageServer.createContext("/profile.png", exchange -> {
            byte[] body = {1, 2, 3};
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        imageServer.start();

        doAnswer(invocation -> {
            transactionActiveWhileUploading.add(TransactionSynchronizationManager.isActualTransactionActive());
            uploadedKeys.add(invocation.<PutObjectRequest>getArgument(0).getKey());
            return null;
        }).when(amazonS3).putObject(any(PutObjectRequest.class));
        when(amazonS3.getUrl(anyString(), anyString())).thenAnswer(invocation -> new URL(S3_URL + invocation.getArgument(1)));
    }

    @AfterEach
    void tearDown() {
        kakaoRestTemplate.setRequestFactory(originalRequestFactory);
        imageServer.stop(0);
        imageServerThreads.shutdownNow();
    }

    @Test
    @DisplayName("카카오 사용자 정보를 받는 동안 DB 트랜잭션을 열어 두지 않는다")
    void callsKakaoOutsideTransaction() throws Exception {
        kakaoReturnsUserWithProfileImage();

        login().andExpect(status().isOk());

        assertThat(transactionActiveWhileCallingKakao).containsExactly(false);
    }

    @Test
    @DisplayName("첫 가입 때 프로필 사진을 S3에 올리는 동안 DB 트랜잭션을 열어 두지 않는다")
    void uploadsProfileImageOutsideTransaction() throws Exception {
        kakaoReturnsUserWithProfileImage();

        login().andExpect(status().isOk()).andExpect(jsonPath("$.data.isNewUser").value(true));

        assertThat(transactionActiveWhileUploading).containsExactly(false);
        assertThat(jdbcTemplate.queryForObject("SELECT image_url FROM users WHERE social_id = ?", String.class, SOCIAL_ID))
                .isEqualTo(S3_URL + uploadedKeys.get(0));
    }

    @Test
    @DisplayName("사용자 저장이 실패하면 방금 올린 프로필 사진을 지운다")
    void deletesUploadedImageWhenSaveFails() throws Exception {
        // S3가 돌려준 주소가 컬럼 길이(255)를 넘어 사용자 저장이 실패하는 상황
        when(amazonS3.getUrl(anyString(), anyString())).thenAnswer(invocation ->
                new URL(S3_URL + invocation.getArgument(1) + "?v=" + "a".repeat(300)));
        kakaoReturnsUserWithProfileImage();

        login().andExpect(status().is5xxServerError());

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE social_id = ?", Integer.class, SOCIAL_ID)).isZero();
        verify(amazonS3, timeout(3_000)).deleteObject(BUCKET, uploadedKeys.get(0));
    }

    @Test
    @DisplayName("이미 가입한 사용자는 다시 로그인해도 프로필 사진을 다시 올리지 않는다")
    void doesNotUploadForExistingUser() throws Exception {
        fixture.traveler(1L);
        jdbcTemplate.update("UPDATE users SET social_id = ? WHERE id = 1", SOCIAL_ID);
        kakaoReturnsUserWithProfileImage();

        login().andExpect(status().isOk()).andExpect(jsonPath("$.data.isNewUser").value(false));

        verify(amazonS3, never()).putObject(any(PutObjectRequest.class));
    }

    private void kakaoReturnsUserWithProfileImage() {
        String profileImageUrl = "http://localhost:" + imageServer.getAddress().getPort() + "/profile.png";
        String userInfoJson = "{\"id\":" + SOCIAL_ID + ",\"kakao_account\":{\"profile\":{\"profile_image_url\":\"" + profileImageUrl + "\"}}}";
        kakao.expect(requestTo(KAKAO_USERINFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    transactionActiveWhileCallingKakao.add(TransactionSynchronizationManager.isActualTransactionActive());
                    return withSuccess(userInfoJson, MediaType.APPLICATION_JSON).createResponse(request);
                });
    }

    private ResultActions login() throws Exception {
        return mockMvc.perform(post("/api/auth/login/KAKAO")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"socialAccessToken\":\"kakao-access-token\"}"));
    }
}
