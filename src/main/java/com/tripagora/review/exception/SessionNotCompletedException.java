package com.tripagora.review.exception;

import com.tripagora.common.code.ReviewErrorCode;

public class SessionNotCompletedException extends ReviewException {
    public SessionNotCompletedException() {
        super(ReviewErrorCode.SESSION_NOT_COMPLETED);
    }
}