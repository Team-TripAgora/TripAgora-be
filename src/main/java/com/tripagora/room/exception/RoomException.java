package com.tripagora.room.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class RoomException extends ApplicationException {
    public RoomException(ResponseCode errorCode) {
        super(errorCode);
    }
}