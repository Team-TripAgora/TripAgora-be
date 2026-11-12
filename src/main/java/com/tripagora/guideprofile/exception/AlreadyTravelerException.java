package com.tripagora.guideprofile.exception;

import com.tripagora.common.code.GuideProfileErrorCode;

public class AlreadyTravelerException extends GuideProfileException {
    public AlreadyTravelerException() {
        super(GuideProfileErrorCode.ALREADY_TRAVELER);
    }
}