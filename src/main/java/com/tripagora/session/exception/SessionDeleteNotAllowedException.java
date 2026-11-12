package com.tripagora.session.exception;

import com.tripagora.common.code.SessionErrorCode;

public class SessionDeleteNotAllowedException extends SessionException {
    public SessionDeleteNotAllowedException() {
        super(SessionErrorCode.SESSION_CANNOT_DELETE);
    }
}