package com.tripagora.session.exception;

import com.tripagora.common.code.SessionErrorCode;

public class InvalidMaxParticipantsException extends SessionException {
    public InvalidMaxParticipantsException() {
        super(SessionErrorCode.INVALID_MAX_PARTICIPANTS);
    }
}