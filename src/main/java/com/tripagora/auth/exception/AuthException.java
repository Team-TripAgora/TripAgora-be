package com.tripagora.auth.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class AuthException extends ApplicationException {
    public AuthException(ResponseCode errorCode) {
        super(errorCode);
    }
}