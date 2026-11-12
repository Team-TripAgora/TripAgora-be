package com.tripagora.room.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class NoticeException extends ApplicationException {
    public NoticeException(ResponseCode errorCode) {
        super(errorCode);
    }
}