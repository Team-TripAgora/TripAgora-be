package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GuideProfileErrorCode implements ResponseCode {
    GUIDE_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "가이드 프로필을 찾을 수 없습니다."),
    ALREADY_GUIDE(HttpStatus.BAD_REQUEST, "이미 가이드 상태입니다."),
    ALREADY_TRAVELER(HttpStatus.BAD_REQUEST, "이미 여행객 상태입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}