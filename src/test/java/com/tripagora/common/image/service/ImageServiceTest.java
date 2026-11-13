package com.tripagora.common.image.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.tripagora.common.image.exception.ImageUploadFailedException;
import com.tripagora.common.image.exception.UnSupportedImageFormatException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// S3 클라이언트를 목으로 바꿔, 실제 AWS에 접속하지 않고 올리려는 키·형식을 확인한다
// URL 다운로드는 테스트 안에서 띄운 HTTP 서버로 확인한다 (외부 네트워크 없음)
class ImageServiceTest {
    private static final String BUCKET = "test-bucket";
    private static final byte[] IMAGE_BYTES = {1, 2, 3};

    private final AmazonS3 amazonS3 = mock(AmazonS3.class);
    private final ImageService imageService = new ImageService(amazonS3);
    private HttpServer server;
    private ExecutorService serverThreads;

    @BeforeEach
    void setUp() throws IOException {
        ReflectionTestUtils.setField(imageService, "bucketName", BUCKET);
        when(amazonS3.getUrl(eq(BUCKET), anyString())).thenAnswer(invocation ->
                new URL("https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/" + invocation.getArgument(1)));

        serverThreads = Executors.newCachedThreadPool();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setExecutor(serverThreads);
        server.createContext("/photo", exchange -> respond(exchange, "image/png", IMAGE_BYTES));
        server.createContext("/page", exchange -> respond(exchange, "text/html; charset=UTF-8", "<html></html>".getBytes()));
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(8_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, "image/png", IMAGE_BYTES);
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        serverThreads.shutdownNow();
    }

    @Test
    @DisplayName("PNG 파일은 .png 이름과 image/png 형식으로 올린다")
    void uploadsPngWithPngExtension() {
        String url = imageService.uploadImage(new MockMultipartFile("image", "photo.png", "image/png", IMAGE_BYTES));

        PutObjectRequest request = uploadedRequest();
        assertThat(request.getKey()).startsWith("images/").endsWith(".png");
        assertThat(request.getMetadata().getContentType()).isEqualTo("image/png");
        assertThat(url).endsWith(request.getKey());
    }

    @Test
    @DisplayName("파일 이름에 확장자가 없으면 Content-Type으로 정한다 (image/jpeg → .jpg)")
    void usesContentTypeWhenFilenameHasNoExtension() {
        imageService.uploadImage(new MockMultipartFile("image", "photo", "image/jpeg", IMAGE_BYTES));

        assertThat(uploadedRequest().getKey()).endsWith(".jpg");
    }

    @Test
    @DisplayName("이미지가 아닌 파일은 올리지 않는다")
    void rejectsNonImageFile() {
        assertThatThrownBy(() -> imageService.uploadImage(new MockMultipartFile("image", "note.txt", "text/plain", IMAGE_BYTES)))
                .isInstanceOf(UnSupportedImageFormatException.class);

        verify(amazonS3, never()).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("URL 이미지는 응답의 Content-Type으로 올린다 (PNG → .png, image/png)")
    void uploadsUrlImageWithResponseContentType() {
        String url = imageService.uploadImageFromUrl(serverUrl("/photo"));

        PutObjectRequest request = uploadedRequest();
        assertThat(request.getKey()).startsWith("images/").endsWith(".png");
        assertThat(request.getMetadata().getContentType()).isEqualTo("image/png");
        assertThat(url).endsWith(request.getKey());
    }

    @Test
    @DisplayName("이미지가 아닌 URL은 올리지 않는다")
    void rejectsNonImageUrl() {
        assertThatThrownBy(() -> imageService.uploadImageFromUrl(serverUrl("/page")))
                .isInstanceOf(UnSupportedImageFormatException.class);

        verify(amazonS3, never()).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("URL 응답이 5초 안에 오지 않으면 기다리지 않고 실패한다")
    void failsWhenUrlRespondsTooSlowly() {
        Instant start = Instant.now();

        assertThatThrownBy(() -> imageService.uploadImageFromUrl(serverUrl("/slow")))
                .isInstanceOf(ImageUploadFailedException.class);

        assertThat(Duration.between(start, Instant.now())).isLessThan(Duration.ofSeconds(7));
        verify(amazonS3, never()).putObject(any(PutObjectRequest.class));
    }

    private String serverUrl(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    private static void respond(HttpExchange exchange, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private PutObjectRequest uploadedRequest() {
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(amazonS3).putObject(request.capture());
        return request.getValue();
    }
}
