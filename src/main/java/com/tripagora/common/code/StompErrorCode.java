package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StompErrorCode implements ResponseCode{
    INVALID_DESTINATION(HttpStatus.BAD_REQUEST, "유효하지 않은 STOMP destination 입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}