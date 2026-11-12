package com.tripagora.user.exception;

import com.tripagora.common.code.UserErrorCode;

public class UserNotFoundException extends UserException {
    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}