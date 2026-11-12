package com.tripagora.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WishlistErrorCode implements ResponseCode {
    ALREADY_IN_WISHLIST(HttpStatus.CONFLICT, "이미 위시리스트에 추가한 여행입니다."),
    WISHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "위시리스트 내역을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}