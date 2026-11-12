package com.tripagora.guideprofile.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class GuideProfileException extends ApplicationException {
    public GuideProfileException(ResponseCode errorCode) {
        super(errorCode);
    }
}