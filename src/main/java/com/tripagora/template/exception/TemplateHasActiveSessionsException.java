package com.tripagora.template.exception;

import com.tripagora.common.code.TemplateErrorCode;

public class TemplateHasActiveSessionsException extends TemplateException {
    public TemplateHasActiveSessionsException() {
        super(TemplateErrorCode.TEMPLATE_HAS_ACTIVE_SESSIONS);
    }
}