package com.tripagora.common.image.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class ImageException extends ApplicationException {
    public ImageException(ResponseCode errorCode) {
        super(errorCode);
    }
}