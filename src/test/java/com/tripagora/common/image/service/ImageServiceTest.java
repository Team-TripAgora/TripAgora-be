package com.tripagora.common.image.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.tripagora.common.image.exception.UnSupportedImageFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URL;

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
class ImageServiceTest {
    private static final String BUCKET = "test-bucket";
    private static final byte[] IMAGE_BYTES = {1, 2, 3};

    private final AmazonS3 amazonS3 = mock(AmazonS3.class);
    private final ImageService imageService = new ImageService(amazonS3);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageService, "bucketName", BUCKET);
        when(amazonS3.getUrl(eq(BUCKET), anyString())).thenAnswer(invocation ->
                new URL("https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/" + invocation.getArgument(1)));
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

    private PutObjectRequest uploadedRequest() {
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(amazonS3).putObject(request.capture());
        return request.getValue();
    }
}
