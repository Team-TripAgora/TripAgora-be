package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TemplateErrorCode implements ResponseCode {
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "플랜을 찾을 수 없습니다."),
    ITINERARY_DAY_SEQUENCE_INVALID(HttpStatus.BAD_REQUEST, "일정은 1일차부터 순차적으로 존재해야 합니다."),
    TEMPLATE_HAS_ACTIVE_SESSIONS(HttpStatus.CONFLICT, "해당 플랜으로 개설된 여행이 존재하여 수정 또는 삭제할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}