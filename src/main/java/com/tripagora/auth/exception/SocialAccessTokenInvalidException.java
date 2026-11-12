package com.tripagora.auth.exception;

import com.tripagora.common.code.AuthErrorCode;

public class SocialAccessTokenInvalidException extends AuthException {
    public SocialAccessTokenInvalidException() {
        super(AuthErrorCode.SOCIAL_ACCESS_TOKEN_INVALID);
    }
}