package com.tripagora.wishlist.exception;

import com.tripagora.common.code.WishlistErrorCode;

public class AlreadyInWishlistException extends WishlistException {
    public AlreadyInWishlistException() {
        super(WishlistErrorCode.ALREADY_IN_WISHLIST);
    }
}