package com.tripagora.wishlist.exception;

import com.tripagora.common.code.WishlistErrorCode;

public class WishlistNotFoundException extends WishlistException {
    public WishlistNotFoundException() {
        super(WishlistErrorCode.WISHLIST_NOT_FOUND);
    }
}