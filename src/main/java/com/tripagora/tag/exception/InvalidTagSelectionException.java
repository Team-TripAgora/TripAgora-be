package com.tripagora.tag.exception;

import com.tripagora.common.code.TagErrorCode;

public class InvalidTagSelectionException extends TagException {
    public InvalidTagSelectionException() {
        super(TagErrorCode.INVALID_TAG_SELECTION);
    }
}