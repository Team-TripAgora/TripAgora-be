package com.tripagora.participation.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class ParticipationException extends ApplicationException {
    public ParticipationException(ResponseCode errorCode) {
        super(errorCode);
    }
}