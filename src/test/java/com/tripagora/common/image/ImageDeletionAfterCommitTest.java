package com.tripagora.common.image;

import com.tripagora.common.image.event.ImageDeletionEvent;
import com.tripagora.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

// 커밋 뒤 S3 삭제: 커밋되면 지우고, 롤백되면 지우지 않는다. 요청 스레드가 아니라 이미지 삭제 전용 스레드에서 지운다
class ImageDeletionAfterCommitTest extends IntegrationTest {
    private static final String BUCKET = "test-bucket";
    private static final String IMAGE_URL = "https://" + BUCKET + ".s3.ap-northeast-2.amazonaws.com/images/old.jpg";

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("커밋되면 S3 파일을 지운다")
    void deletesAfterCommit() {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new ImageDeletionEvent(List.of(IMAGE_URL))));

        verify(amazonS3, timeout(3_000)).deleteObject(BUCKET, "images/old.jpg");
    }

    @Test
    @DisplayName("롤백되면 S3 파일을 지우지 않는다")
    void keepsFileOnRollback() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new ImageDeletionEvent(List.of(IMAGE_URL)));
            status.setRollbackOnly();
        });

        verify(amazonS3, after(1_000).never()).deleteObject(anyString(), anyString());
    }

    @Test
    @DisplayName("요청 스레드가 아니라 이미지 삭제 전용 스레드에서 지운다")
    void deletesOnSeparateThread() throws Exception {
        CompletableFuture<String> deletingThread = new CompletableFuture<>();
        doAnswer(invocation -> {
            deletingThread.complete(Thread.currentThread().getName());
            return null;
        }).when(amazonS3).deleteObject(anyString(), anyString());

        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new ImageDeletionEvent(List.of(IMAGE_URL))));

        assertThat(deletingThread.get(3, TimeUnit.SECONDS)).startsWith("image-delete-");
    }

    @Test
    @DisplayName("S3 삭제가 실패해도 요청(트랜잭션)은 성공한다")
    void ignoresDeletionFailure() {
        doThrow(new RuntimeException("S3 오류")).when(amazonS3).deleteObject(anyString(), anyString());

        assertThatCode(() -> transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new ImageDeletionEvent(List.of(IMAGE_URL)))))
                .doesNotThrowAnyException();

        verify(amazonS3, timeout(3_000)).deleteObject(BUCKET, "images/old.jpg");
    }
}
