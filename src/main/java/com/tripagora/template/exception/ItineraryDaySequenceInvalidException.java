package com.tripagora.template.exception;

import com.tripagora.common.code.TemplateErrorCode;

public class ItineraryDaySequenceInvalidException extends TemplateException {
    public ItineraryDaySequenceInvalidException() {
        super(TemplateErrorCode.ITINERARY_DAY_SEQUENCE_INVALID);
    }
}