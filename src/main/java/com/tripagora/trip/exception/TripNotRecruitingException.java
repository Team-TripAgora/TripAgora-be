package com.tripagora.trip.exception;

import com.tripagora.common.code.TripErrorCode;

public class TripNotRecruitingException extends TripException {
    public TripNotRecruitingException() {
        super(TripErrorCode.TRIP_NOT_RECRUITING);
    }
}