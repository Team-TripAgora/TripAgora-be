package com.tripagora.common.image.exception;

import com.tripagora.common.code.ImageErrorCode;

public class UnSupportedImageFormatException extends ImageException {
    public UnSupportedImageFormatException() {
        super(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT);
    }
}