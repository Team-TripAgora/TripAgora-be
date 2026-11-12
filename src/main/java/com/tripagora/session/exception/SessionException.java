package com.tripagora.session.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class SessionException extends ApplicationException {
    public SessionException(ResponseCode errorCode) {
        super(errorCode);
    }
}