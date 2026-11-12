package com.tripagora.participation.exception;

import com.tripagora.common.code.ParticipationErrorCode;

public class ParticipationNotFoundException extends ParticipationException {
    public ParticipationNotFoundException() {
        super(ParticipationErrorCode.PARTICIPATION_NOT_FOUND);
    }
}