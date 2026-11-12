package com.tripagora.auth.exception;

import com.tripagora.common.code.AuthErrorCode;

public class SocialJsonParseErrorException extends AuthException {
    public SocialJsonParseErrorException() {
        super(AuthErrorCode.SOCIAL_JSON_PARSE_ERROR);
    }
}