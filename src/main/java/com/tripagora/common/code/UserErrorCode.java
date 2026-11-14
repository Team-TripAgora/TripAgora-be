package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ResponseCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 존재하는 닉네임입니다."),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "올바르지 않은 닉네임 형식입니다."),
    WITHDRAWAL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "진행 중이거나 예정된 여행이 있어 탈퇴할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}