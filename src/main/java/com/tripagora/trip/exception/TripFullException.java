package com.tripagora.trip.exception;

import com.tripagora.common.code.TripErrorCode;

public class TripFullException extends TripException {
    public TripFullException() {
        super(TripErrorCode.TRIP_FULL);
    }
}