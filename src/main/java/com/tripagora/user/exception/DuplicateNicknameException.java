package com.tripagora.user.exception;

import com.tripagora.common.code.UserErrorCode;

public class DuplicateNicknameException extends UserException {
    public DuplicateNicknameException() {
        super(UserErrorCode.DUPLICATE_NICKNAME);
    }
}