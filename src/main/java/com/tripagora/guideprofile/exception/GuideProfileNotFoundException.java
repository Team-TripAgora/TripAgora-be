package com.tripagora.guideprofile.exception;

import com.tripagora.common.code.GuideProfileErrorCode;

public class GuideProfileNotFoundException extends GuideProfileException {
    public GuideProfileNotFoundException() {
        super(GuideProfileErrorCode.GUIDE_PROFILE_NOT_FOUND);
    }
}