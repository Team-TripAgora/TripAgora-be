package com.tripagora.auth.exception;

import com.tripagora.common.code.AuthErrorCode;

public class RefreshTokenInvalidException extends AuthException {
    public RefreshTokenInvalidException() {
        super(AuthErrorCode.REFRESH_TOKEN_INVALID);
    }
}