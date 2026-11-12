package com.tripagora.review.exception;

import com.tripagora.common.code.ReviewErrorCode;

public class ReviewNotFoundException extends ReviewException {
    public ReviewNotFoundException() {
        super(ReviewErrorCode.REVIEW_NOT_FOUND);
    }
}