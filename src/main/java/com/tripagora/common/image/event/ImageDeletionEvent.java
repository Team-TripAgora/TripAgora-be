package com.tripagora.common.image.event;

import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

// 트랜잭션 안에서 "커밋되면 S3에서 지울 이미지"를 남긴다. 롤백되면 아무것도 지우지 않는다
public record ImageDeletionEvent(List<String> imageUrls) {
    // 사진이 없던 경우(null, 빈 문자열)는 지울 것이 없으므로 뺀다
    public ImageDeletionEvent {
        imageUrls = imageUrls.stream().filter(StringUtils::hasText).toList();
    }

    public static ImageDeletionEvent of(String imageUrl) {
        return new ImageDeletionEvent(Collections.singletonList(imageUrl));
    }
}
