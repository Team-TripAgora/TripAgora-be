package com.tripagora.tag.exception;

import com.tripagora.common.code.TagErrorCode;

public class MinimumTagsRequiredException extends TagException {
    public MinimumTagsRequiredException() {
        super(TagErrorCode.MINIMUM_TAGS_REQUIRED);
    }
}