package com.tripagora.template.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public record ItineraryItemResponse(
        Long id,
        Integer day,
        String location,
        String content,
        @JsonFormat(pattern = "HH:mm:ss")
        LocalTime startTime,
        Double latitude,
        Double longitude) {}