package com.tripagora.session.exception;

import com.tripagora.common.code.SessionErrorCode;

public class SessionAlreadyEndedException extends SessionException {
    public SessionAlreadyEndedException() {
        super(SessionErrorCode.SESSION_ALREADY_ENDED);
    }
}