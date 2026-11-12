package com.tripagora.review.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.code.ReviewErrorCode;

public class ReviewNotAllowedException extends ReviewException{
    public ReviewNotAllowedException() {
        super(ReviewErrorCode.REVIEW_NOT_ALLOWED);
    }
}