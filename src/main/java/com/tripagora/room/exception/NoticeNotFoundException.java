package com.tripagora.room.exception;

import com.tripagora.common.code.NoticeErrorCode;

public class NoticeNotFoundException extends NoticeException {
    public NoticeNotFoundException() {
        super(NoticeErrorCode.NOTICE_NOT_FOUND);
    }
}