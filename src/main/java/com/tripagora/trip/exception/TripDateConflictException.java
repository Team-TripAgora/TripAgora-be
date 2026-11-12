package com.tripagora.trip.exception;

import com.tripagora.common.code.TripErrorCode;

public class TripDateConflictException extends TripException {
    public TripDateConflictException() {
        super(TripErrorCode.TRIP_DATE_CONFLICT);
    }
}