package com.tripagora.stomp.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class StompException extends ApplicationException {
    public StompException(ResponseCode errorCode) {
        super(errorCode);
    }
}