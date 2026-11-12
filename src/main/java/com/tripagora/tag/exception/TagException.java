package com.tripagora.tag.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class TagException extends ApplicationException {
    public TagException(ResponseCode errorCode) {
        super(errorCode);
    }
}