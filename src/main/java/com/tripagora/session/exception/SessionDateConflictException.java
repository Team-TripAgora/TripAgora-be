package com.tripagora.session.exception;

import com.tripagora.common.code.SessionErrorCode;

public class SessionDateConflictException extends SessionException {
    public SessionDateConflictException() {
        super(SessionErrorCode.SESSION_DATE_CONFLICT);
    }
}