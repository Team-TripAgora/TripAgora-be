package com.tripagora.template.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class TemplateException extends ApplicationException {
    public TemplateException(ResponseCode errorCode) {
        super(errorCode);
    }
}