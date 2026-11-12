package com.tripagora.auth.exception;

import com.tripagora.common.code.AuthErrorCode;

public class UnAuthorizedAccessException extends AuthException {
    public UnAuthorizedAccessException() {
        super(AuthErrorCode.UNAUTHORIZED_ACCESS);
    }
}