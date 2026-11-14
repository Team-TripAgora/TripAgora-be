package com.tripagora.common.image.event;

import com.tripagora.common.config.AsyncConfig;
import com.tripagora.common.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageDeletionEventListener {
    private final ImageService imageService;

    // DB가 커밋된 뒤에만 S3 파일을 지운다. 응답과 DB 연결을 붙잡지 않도록 이미지 삭제 전용 스레드에서 실행한다
    @Async(AsyncConfig.IMAGE_DELETION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deleteAfterCommit(ImageDeletionEvent event) {
        event.imageUrls().forEach(this::deleteQuietly);
    }

    // 요청은 이미 성공했으므로 삭제 실패는 로그만 남긴다 (파일이 남을 뿐 깨진 이미지는 생기지 않는다)
    private void deleteQuietly(String imageUrl) {
        try {
            imageService.deleteImage(imageUrl);
        } catch (RuntimeException e) {
            log.warn("S3 이미지 삭제 실패, 파일이 남는다: {}", imageUrl, e);
        }
    }
}
