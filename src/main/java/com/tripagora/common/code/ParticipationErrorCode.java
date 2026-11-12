package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ParticipationErrorCode implements ResponseCode {
    PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND, "여행 참여를 찾을 수 없습니다."),
    ALREADY_PARTICIPATING(HttpStatus.CONFLICT, "이미 해당 여행에 참여 신청을 했습니다."),
    CANNOT_PARTICIPATE_IN_OWN_TRIP(HttpStatus.BAD_REQUEST, "자신이 개설한 여행에는 참여할 수 없습니다."),
    PARTICIPATION_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "인원 모집중 혹은 종료된 여행만 참여 취소를 할 수 있습니다."),
    CONFLICTING_PARTICIPATION(HttpStatus.BAD_REQUEST, "해당 날짜에 이미 참여중인 다른 여행이 존재합니다.");

    private final HttpStatus httpStatus;
    private final String message;
}