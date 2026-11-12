package com.tripagora.guideProfile.exception;

import com.tripagora.common.code.GuideProfileErrorCode;

public class AlreadyGuideException extends GuideProfileException {
    public AlreadyGuideException() {
        super(GuideProfileErrorCode.ALREADY_GUIDE);
    }
}