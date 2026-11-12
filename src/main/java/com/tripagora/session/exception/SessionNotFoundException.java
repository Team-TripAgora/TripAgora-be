package com.tripagora.session.exception;

import com.tripagora.common.code.SessionErrorCode;

public class SessionNotFoundException extends SessionException {
    public SessionNotFoundException() {
        super(SessionErrorCode.SESSION_NOT_FOUND);
    }
}