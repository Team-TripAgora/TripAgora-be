package com.tripagora.user;

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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 프로필 사진(사용자·가이드) 변경: 새 사진은 트랜잭션 전에 올리고, 옛 사진은 DB가 커밋된 뒤에만 지운다
class ProfileImageReplaceTest extends IntegrationTest {
    private static final String BUCKET = "test-bucket";
    private static final String S3_URL = "https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/";
    private static final String OLD_KEY = "images/old.jpg";
    private static final long USER = 1L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> uploadedKeys = new CopyOnWriteArrayList<>();
    private final List<Boolean> transactionActiveWhileUploading = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        fixture.guide(USER);
        jdbcTemplate.update("UPDATE users SET image_url = ? WHERE id = ?", S3_URL + OLD_KEY, USER);
        jdbcTemplate.update("UPDATE guide_profile SET image_url = ? WHERE id = ?", S3_URL + OLD_KEY, USER);
        doAnswer(invocation -> {
            transactionActiveWhileUploading.add(TransactionSynchronizationManager.isActualTransactionActive());
            uploadedKeys.add(invocation.<PutObjectRequest>getArgument(0).getKey());
            return null;
        }).when(amazonS3).putObject(any(PutObjectRequest.class));
        when(amazonS3.getUrl(anyString(), anyString())).thenAnswer(invocation -> new URL(S3_URL + invocation.getArgument(1)));
    }

    @Test
    @DisplayName("프로필 사진을 바꾸면 새 사진으로 저장하고, 옛 사진은 커밋 뒤에 지운다")
    void replacesUserImage() throws Exception {
        updateUserImage().andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT image_url FROM users WHERE id = ?", String.class, USER))
                .isEqualTo(S3_URL + uploadedKeys.get(0));
        verify(amazonS3, timeout(3_000)).deleteObject(BUCKET, OLD_KEY);
    }

    @Test
    @DisplayName("DB 저장이 실패하면 옛 사진은 지우지 않고, 방금 올린 새 사진을 지운다")
    void keepsOldImageWhenSaveFails() throws Exception {
        // S3가 돌려준 주소가 컬럼 길이(255)를 넘어 DB 저장이 실패하는 상황
        when(amazonS3.getUrl(anyString(), anyString())).thenAnswer(invocation ->
                new URL(S3_URL + invocation.getArgument(1) + "?v=" + "a".repeat(300)));

        updateUserImage().andExpect(status().is5xxServerError());

        verify(amazonS3, after(1_000).never()).deleteObject(BUCKET, OLD_KEY);
        assertThat(jdbcTemplate.queryForObject("SELECT image_url FROM users WHERE id = ?", String.class, USER))
                .isEqualTo(S3_URL + OLD_KEY);
        verify(amazonS3, timeout(3_000)).deleteObject(BUCKET, uploadedKeys.get(0));
    }

    @Test
    @DisplayName("프로필 사진을 S3에 올리는 동안 DB 트랜잭션을 열어 두지 않는다")
    void uploadsUserImageOutsideTransaction() throws Exception {
        updateUserImage().andExpect(status().isOk());

        assertThat(transactionActiveWhileUploading).containsExactly(false);
    }

    @Test
    @DisplayName("가이드 사진을 바꾸면 옛 사진은 커밋 뒤에 지우고, 올리는 동안 DB 트랜잭션을 열어 두지 않는다")
    void replacesGuideImage() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/guide-profiles/me/image")
                        .file(image())
                        .header("Authorization", bearer(USER, Role.GUIDE)))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT image_url FROM guide_profile WHERE id = ?", String.class, USER))
                .isEqualTo(S3_URL + uploadedKeys.get(0));
        verify(amazonS3, timeout(3_000)).deleteObject(BUCKET, OLD_KEY);
        assertThat(transactionActiveWhileUploading).containsExactly(false);
    }

    private ResultActions updateUserImage() throws Exception {
        return mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me/profile-image")
                .file(image())
                .header("Authorization", bearer(USER, Role.GUIDE)));
    }

    private static MockMultipartFile image() {
        return new MockMultipartFile("imageFile", "me.png", "image/png", new byte[]{1, 2, 3});
    }
}
