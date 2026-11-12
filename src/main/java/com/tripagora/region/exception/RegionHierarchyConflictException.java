package com.tripagora.region.exception;

import com.tripagora.common.code.RegionErrorCode;

public class RegionHierarchyConflictException extends RegionException {
    public RegionHierarchyConflictException() {
        super(RegionErrorCode.REGION_HIERARCHY_CONFLICT);
    }
}