package com.tripagora.participation.exception;

import com.tripagora.common.code.ParticipationErrorCode;

public class ParticipationCancelNotAllowedException extends ParticipationException {
    public ParticipationCancelNotAllowedException() {
        super(ParticipationErrorCode.PARTICIPATION_CANCEL_NOT_ALLOWED);
    }
}