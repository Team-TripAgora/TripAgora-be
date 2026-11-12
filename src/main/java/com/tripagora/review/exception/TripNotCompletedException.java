package com.tripagora.review.exception;

import com.tripagora.common.code.ReviewErrorCode;

public class TripNotCompletedException extends ReviewException {
    public TripNotCompletedException() {
        super(ReviewErrorCode.TRIP_NOT_COMPLETED);
    }
}