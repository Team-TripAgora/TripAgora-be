package com.tripagora.trip.exception;

import com.tripagora.common.code.TripErrorCode;

public class TripDeleteNotAllowedException extends TripException {
    public TripDeleteNotAllowedException() {
        super(TripErrorCode.TRIP_CANNOT_DELETE);
    }
}