package com.tripagora.trip.exception;

import com.tripagora.common.code.TripErrorCode;

public class TripNotFoundException extends TripException {
    public TripNotFoundException() {
        super(TripErrorCode.TRIP_NOT_FOUND);
    }
}