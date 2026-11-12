package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TripErrorCode implements ResponseCode{
    TRIP_NOT_FOUND(HttpStatus.NOT_FOUND, "여행을 찾을 수 없습니다."),
    TRIP_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "인원을 모집중인 여행이 아닙니다."),
    INVALID_MAX_PARTICIPANTS(HttpStatus.BAD_REQUEST, "현재 신청 인원보다 적은 수로 정원을 수정할 수 없습니다."),
    TRIP_DATE_CONFLICT(HttpStatus.BAD_REQUEST, "이미 해당 날짜에 진행 중인 여행이 존재합니다."),
    TRIP_FULL(HttpStatus.CONFLICT, "여행 참여 인원이 모두 찼습니다."),
    TRIP_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "진행중인 여행에만 가능한 요청입니다."),
    TRIP_ALREADY_ENDED(HttpStatus.BAD_REQUEST, "여행 종료날짜가 이미 지났습니다. 여행을 새로 개설하세요."),
    TRIP_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "모집중인 여행만 삭제 가능합니다.");

    private final HttpStatus httpStatus;
    private final String message;
}