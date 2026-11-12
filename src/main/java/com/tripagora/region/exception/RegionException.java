package com.tripagora.region.exception;

import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.exception.ApplicationException;

public class RegionException extends ApplicationException {
    public RegionException(ResponseCode errorCode) {
        super(errorCode);
    }
}