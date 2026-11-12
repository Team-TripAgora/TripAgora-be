package com.tripagora.session.exception;

import com.tripagora.common.code.SessionErrorCode;

public class SessionNotInProgressException extends SessionException {
    public SessionNotInProgressException() {
        super(SessionErrorCode.SESSION_NOT_IN_PROGRESS);
    }
}