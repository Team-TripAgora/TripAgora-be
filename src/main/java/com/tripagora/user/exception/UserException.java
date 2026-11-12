package com.tripagora.user.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class UserException extends ApplicationException {
    public UserException(ResponseCode errorCode) {
        super(errorCode);
    }
}