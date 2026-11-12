package com.tripagora.session.exception;

import com.tripagora.common.code.SessionErrorCode;

public class SessionNotRecruitingException extends SessionException {
    public SessionNotRecruitingException() {
        super(SessionErrorCode.SESSION_NOT_RECRUITING);
    }
}