package com.tripagora.participation.exception;

import com.tripagora.common.code.ParticipationErrorCode;

public class AlreadyParticipatingException extends ParticipationException {
    public AlreadyParticipatingException() {
        super(ParticipationErrorCode.ALREADY_PARTICIPATING);
    }
}