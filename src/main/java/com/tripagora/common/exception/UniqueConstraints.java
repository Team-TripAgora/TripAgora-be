package com.tripagora.common.exception;

import org.springframework.dao.DataIntegrityViolationException;

// DB unique 제약 위반을 제약 이름으로 구분한다 (예: 동시에 들어온 같은 요청 중 늦은 쪽)
public final class UniqueConstraints {

    private UniqueConstraints() {
    }

    public static boolean isViolated(DataIntegrityViolationException e, String constraintName) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(constraintName);
    }
}
