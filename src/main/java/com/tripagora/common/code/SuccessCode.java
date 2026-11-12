package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements ResponseCode {
    OK(HttpStatus.OK, "요청이 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "리소스가 생성되었습니다."),
    NO_CONTENT(HttpStatus.NO_CONTENT, "요청이 처리되었습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}