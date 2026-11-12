package com.tripagora.tag.exception;

import com.tripagora.common.code.TagErrorCode;

public class MaximumTagsExceededException extends TagException {
    public MaximumTagsExceededException() {
        super(TagErrorCode.MAXIMUM_TAGS_EXCEED);
    }
}