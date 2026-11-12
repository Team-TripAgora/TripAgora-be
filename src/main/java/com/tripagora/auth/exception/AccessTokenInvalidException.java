package com.tripagora.auth.exception;

import com.tripagora.common.code.AuthErrorCode;

public class AccessTokenInvalidException extends AuthException {
    public AccessTokenInvalidException() {
        super(AuthErrorCode.ACCESS_TOKEN_INVALID);
    }
}