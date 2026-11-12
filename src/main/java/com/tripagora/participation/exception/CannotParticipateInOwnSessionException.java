package com.tripagora.participation.exception;

import com.tripagora.common.code.ParticipationErrorCode;

public class CannotParticipateInOwnSessionException extends ParticipationException {
    public CannotParticipateInOwnSessionException() {
        super(ParticipationErrorCode.CANNOT_PARTICIPATE_IN_OWN_SESSION);
    }
}