package com.tripagora.user.exception;

import com.tripagora.common.code.UserErrorCode;

public class WithdrawalNotAllowedException extends UserException {
    public WithdrawalNotAllowedException() {
        super(UserErrorCode.WITHDRAWAL_NOT_ALLOWED);
    }
}
