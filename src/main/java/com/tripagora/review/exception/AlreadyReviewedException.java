package com.tripagora.review.exception;

import com.tripagora.common.code.ReviewErrorCode;

public class AlreadyReviewedException extends ReviewException {
    public AlreadyReviewedException() {
        super(ReviewErrorCode.ALREADY_REVIEWED);
    }
}