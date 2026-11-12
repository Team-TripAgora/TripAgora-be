package com.tripagora.session.exception;

import com.tripagora.common.code.SessionErrorCode;

public class SessionFullException extends SessionException {
    public SessionFullException() {
        super(SessionErrorCode.SESSION_FULL);
    }
}