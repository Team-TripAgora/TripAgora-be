package com.tripagora.trip.exception;

import com.tripagora.common.code.TripErrorCode;

public class TripNotInProgressException extends TripException {
    public TripNotInProgressException() {
        super(TripErrorCode.TRIP_NOT_IN_PROGRESS);
    }
}