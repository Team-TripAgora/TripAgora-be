package com.tripagora.common.image.exception;

import com.tripagora.common.code.ImageErrorCode;

public class ImageUploadFailedException extends ImageException {
    public ImageUploadFailedException() {
        super(ImageErrorCode.IMAGE_UPLOAD_FAILED);
    }
}