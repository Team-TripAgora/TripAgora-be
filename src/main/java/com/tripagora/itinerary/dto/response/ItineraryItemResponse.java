package com.tripagora.itinerary.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tripagora.itinerary.entity.Itinerary;

import java.time.LocalTime;

public record ItineraryItemResponse(
        Long id,
        Integer day,
        String location,
        String content,
        @JsonFormat(pattern = "HH:mm:ss")
        LocalTime startTime,
        Double latitude,
        Double longitude) {

    public static ItineraryItemResponse from(Itinerary itinerary) {
        return new ItineraryItemResponse(
                itinerary.getId(),
                itinerary.getDay(),
                itinerary.getLocation(),
                itinerary.getContent(),
                itinerary.getStartTime(),
                itinerary.getLatitude(),
                itinerary.getLongitude());
    }
}