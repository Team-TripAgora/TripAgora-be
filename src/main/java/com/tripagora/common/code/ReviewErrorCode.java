package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ResponseCode {

    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    ALREADY_REVIEWED(HttpStatus.CONFLICT, "이미 이 여행에 대한 리뷰를 작성했습니다."),
    REVIEW_NOT_ALLOWED(HttpStatus.FORBIDDEN, "여행에 참여한 여행객만 리뷰를 작성할 수 있습니다."),
    TRIP_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "종료된 여행에 대해서만 리뷰를 작성할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}