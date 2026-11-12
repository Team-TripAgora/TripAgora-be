package com.tripagora.common.image.exception;

import com.tripagora.common.code.ImageErrorCode;

public class InvalidImageUrlFormatException extends ImageException {
    public InvalidImageUrlFormatException() {
        super(ImageErrorCode.INVALID_IMAGE_URL_FORMAT);
    }
}