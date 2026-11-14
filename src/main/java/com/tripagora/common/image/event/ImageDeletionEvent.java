package com.tripagora.common.image.event;

import java.util.List;

// 트랜잭션 안에서 "커밋되면 S3에서 지울 이미지"를 남긴다. 롤백되면 아무것도 지우지 않는다
public record ImageDeletionEvent(List<String> imageUrls) {
}
