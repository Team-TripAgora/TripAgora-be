package com.tripagora.participation.exception;

import com.tripagora.common.code.ParticipationErrorCode;

public class ConflictingParticipationException extends ParticipationException {
    public ConflictingParticipationException() {
        super(ParticipationErrorCode.CONFLICTING_PARTICIPATION);
    }
}