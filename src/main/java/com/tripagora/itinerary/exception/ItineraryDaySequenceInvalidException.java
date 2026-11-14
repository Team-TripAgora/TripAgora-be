package com.tripagora.itinerary.exception;

import com.tripagora.common.code.TemplateErrorCode;
import com.tripagora.template.exception.TemplateException;

public class ItineraryDaySequenceInvalidException extends TemplateException {
    public ItineraryDaySequenceInvalidException() {
        super(TemplateErrorCode.ITINERARY_DAY_SEQUENCE_INVALID);
    }
}