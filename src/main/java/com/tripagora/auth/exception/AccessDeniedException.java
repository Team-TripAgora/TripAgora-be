package com.tripagora.auth.exception;

import com.tripagora.common.code.AuthErrorCode;

public class AccessDeniedException extends AuthException {
    public AccessDeniedException() {
        super(AuthErrorCode.ACCESS_DENIED);
    }
}