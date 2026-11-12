package com.tripagora.stomp.exception;

import com.tripagora.common.code.StompErrorCode;

public class InvalidDestinationException extends StompException {
    public InvalidDestinationException() {
        super(StompErrorCode.INVALID_DESTINATION);
    }
}