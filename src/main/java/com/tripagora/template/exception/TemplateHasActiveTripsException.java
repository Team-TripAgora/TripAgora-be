package com.tripagora.template.exception;

import com.tripagora.common.code.TemplateErrorCode;

public class TemplateHasActiveTripsException extends TemplateException {
    public TemplateHasActiveTripsException() {
        super(TemplateErrorCode.TEMPLATE_HAS_ACTIVE_TRIPS);
    }
}