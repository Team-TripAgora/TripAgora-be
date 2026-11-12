package com.tripagora.trip.exception;

import com.tripagora.common.code.TripErrorCode;

public class TripAlreadyEndedException extends TripException {
    public TripAlreadyEndedException() {
        super(TripErrorCode.TRIP_ALREADY_ENDED);
    }
}