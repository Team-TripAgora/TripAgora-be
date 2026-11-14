package com.tripagora.participation.exception;

import com.tripagora.common.code.ParticipationErrorCode;

public class GuideCannotCancelParticipationException extends ParticipationException {
    public GuideCannotCancelParticipationException() {
        super(ParticipationErrorCode.GUIDE_CANNOT_CANCEL_PARTICIPATION);
    }
}
