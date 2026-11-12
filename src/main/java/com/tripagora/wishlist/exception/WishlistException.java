package com.tripagora.wishlist.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class WishlistException extends ApplicationException {
    public WishlistException(ResponseCode errorCode) {
        super(errorCode);
    }
}