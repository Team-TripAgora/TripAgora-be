package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RegionErrorCode implements ResponseCode {
    INVALID_REGION_SELECTION(HttpStatus.BAD_REQUEST, "존재하지 않거나 중복된 지역이 포함되어 있습니다."),
    REGION_HIERARCHY_CONFLICT(HttpStatus.BAD_REQUEST, "상위 지역과 하위 지역을 함께 선택할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}