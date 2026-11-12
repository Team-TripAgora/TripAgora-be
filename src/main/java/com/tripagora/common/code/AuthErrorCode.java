package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ResponseCode{
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "인증되지 않은 접근입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    ACCESS_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    SOCIAL_ACCESS_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 소셜 로그인 엑세스 토큰입니다."),
    SOCIAL_JSON_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "소셜 로그인 응답 파싱 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}