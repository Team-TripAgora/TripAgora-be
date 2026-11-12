package com.tripagora.template.exception;

import com.tripagora.common.code.TemplateErrorCode;

public class TemplateNotFoundException extends TemplateException {
    public TemplateNotFoundException() {
        super(TemplateErrorCode.TEMPLATE_NOT_FOUND);
    }
}