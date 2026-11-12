package com.tripagora.common.image.exception;

import com.tripagora.common.code.ImageErrorCode;

public class ImageDeletionFailedException extends ImageException {
    public ImageDeletionFailedException() {
        super(ImageErrorCode.IMAGE_DELETION_FAILED);
    }
}