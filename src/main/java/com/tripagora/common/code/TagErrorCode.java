package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TagErrorCode implements ResponseCode {
    MINIMUM_TAGS_REQUIRED(HttpStatus.BAD_REQUEST, "관심사는 최소 3개 이상 선택해야 합니다."),
    MAXIMUM_TAGS_EXCEED(HttpStatus.BAD_REQUEST, "관심사는 최대 5개까지 선택 가능합니다."),
    INVALID_TAG_SELECTION(HttpStatus.BAD_REQUEST, "존재하지 않거나 중복된 관심사가 포함되어 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}