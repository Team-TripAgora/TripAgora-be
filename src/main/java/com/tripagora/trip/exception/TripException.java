package com.tripagora.trip.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class TripException extends ApplicationException {
    public TripException(ResponseCode errorCode) {
        super(errorCode);
    }
}