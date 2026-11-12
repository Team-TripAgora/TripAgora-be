package com.tripagora.user.exception;

import com.tripagora.common.code.UserErrorCode;

public class InvalidNicknameFormatException extends UserException {
    public InvalidNicknameFormatException() {
        super(UserErrorCode.INVALID_NICKNAME_FORMAT);
    }
}