package com.tripagora.region.exception;

import com.tripagora.common.code.RegionErrorCode;

public class InvalidRegionSelectionException extends RegionException {
    public InvalidRegionSelectionException() {
        super(RegionErrorCode.INVALID_REGION_SELECTION);
    }
}