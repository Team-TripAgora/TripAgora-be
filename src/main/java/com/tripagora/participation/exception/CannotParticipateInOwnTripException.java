package com.tripagora.participation.exception;

import com.tripagora.common.code.ParticipationErrorCode;

public class CannotParticipateInOwnTripException extends ParticipationException {
    public CannotParticipateInOwnTripException() {
        super(ParticipationErrorCode.CANNOT_PARTICIPATE_IN_OWN_TRIP);
    }
}