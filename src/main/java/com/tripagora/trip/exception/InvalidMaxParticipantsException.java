package com.tripagora.trip.exception;

import com.tripagora.common.code.TripErrorCode;

public class InvalidMaxParticipantsException extends TripException {
    public InvalidMaxParticipantsException() {
        super(TripErrorCode.INVALID_MAX_PARTICIPANTS);
    }
}