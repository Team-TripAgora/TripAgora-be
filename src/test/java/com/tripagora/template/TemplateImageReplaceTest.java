package com.tripagora.template;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.tripagora.support.IntegrationTest;
import com.tripagora.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 템플릿 이미지 교체: 새 파일은 트랜잭션 전에 올리고, 원본은 DB가 커밋된 뒤에만 지운다
class TemplateImageReplaceTest extends IntegrationTest {
    private static final String BUCKET = "test-bucket";
    private static final String S3_URL = "https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/";
    private static final String OLD_KEY = "images/old.jpg";
    private static final long GUIDE = 1L;
    private static final long TEMPLATE = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 올린 파일의 키와, 올리는 순간 DB 트랜잭션이 열려 있었는지
    private final List<String> uploadedKeys = new CopyOnWriteArrayList<>();
    private final List<Boolean> transactionActiveWhileUploading = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        fixture.guide(GUIDE);
        fixture.template(TEMPLATE, GUIDE, 1);
        jdbcTemplate.update("INSERT INTO template_image (created_at, modified_at, display_order, image_url, template_id) " +
                "VALUES (NOW(6), NOW(6), 1, ?, ?)", S3_URL + OLD_KEY, TEMPLATE);
        when(amazonS3.getUrl(anyString(), anyString())).thenAnswer(invocation -> new URL(S3_URL + invocation.getArgument(1)));
    }

    @Test
    @DisplayName("교체가 커밋되면 새 이미지로 바뀌고, 원본은 그 뒤에 지운다")
    void replacesAndDeletesOriginalAfterCommit() throws Exception {
        recordUploads(false);

        replaceImages().andExpect(status().isOk());

        assertThat(savedImageUrls()).hasSize(2).allMatch(url -> url.startsWith(S3_URL + "images/") && !url.endsWith(OLD_KEY));
        verify(amazonS3, timeout(3_000)).deleteObject(BUCKET, OLD_KEY);
    }

    @Test
    @DisplayName("새 이미지 업로드 중 실패하면 원본은 지우지 않고 DB도 그대로 두며, 먼저 올라간 새 파일은 지운다")
    void keepsOriginalWhenUploadFails() throws Exception {
        recordUploads(true);

        replaceImages().andExpect(status().is5xxServerError());

        verify(amazonS3, after(1_000).never()).deleteObject(BUCKET, OLD_KEY);
        assertThat(savedImageUrls()).containsExactly(S3_URL + OLD_KEY);
        assertThat(uploadedKeys).hasSize(1);
        verify(amazonS3, timeout(3_000)).deleteObject(BUCKET, uploadedKeys.get(0));
    }

    @Test
    @DisplayName("S3에 올리는 동안 DB 트랜잭션을 열어 두지 않는다")
    void uploadsOutsideTransaction() throws Exception {
        recordUploads(false);

        replaceImages().andExpect(status().isOk());

        assertThat(transactionActiveWhileUploading).containsOnly(false);
    }

    @Test
    @DisplayName("남의 템플릿이면 S3에 아무것도 올리지 않는다")
    void doesNotUploadForOthersTemplate() throws Exception {
        fixture.guide(2L);

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/templates/" + TEMPLATE + "/images")
                        .file(image("a.png")).file(image("b.png"))
                        .header("Authorization", bearer(2L, Role.GUIDE)))
                .andExpect(status().isForbidden());

        verify(amazonS3, never()).putObject(any(PutObjectRequest.class));
    }

    // 업로드 키와 트랜잭션 상태를 기록한다. failSecond면 두 번째 업로드에서 S3 오류를 낸다
    private void recordUploads(boolean failSecond) {
        doAnswer(invocation -> {
            transactionActiveWhileUploading.add(TransactionSynchronizationManager.isActualTransactionActive());
            if (failSecond && uploadedKeys.size() == 1) {
                throw new AmazonServiceException("S3 업로드 실패");
            }
            uploadedKeys.add(invocation.<PutObjectRequest>getArgument(0).getKey());
            return null;
        }).when(amazonS3).putObject(any(PutObjectRequest.class));
    }

    private ResultActions replaceImages() throws Exception {
        return mockMvc.perform(multipart(HttpMethod.PATCH, "/api/templates/" + TEMPLATE + "/images")
                .file(image("a.png")).file(image("b.png"))
                .header("Authorization", bearer(GUIDE, Role.GUIDE)));
    }

    private static MockMultipartFile image(String filename) {
        return new MockMultipartFile("images", filename, "image/png", new byte[]{1, 2, 3});
    }

    private List<String> savedImageUrls() {
        return jdbcTemplate.queryForList("SELECT image_url FROM template_image WHERE template_id = ?", String.class, TEMPLATE);
    }
}
