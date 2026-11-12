package com.tripagora.review.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class ReviewException extends ApplicationException {
    public ReviewException(ResponseCode errorCode) {
        super(errorCode);
    }
}