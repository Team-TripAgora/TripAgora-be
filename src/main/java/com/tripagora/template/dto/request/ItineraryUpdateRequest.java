package com.tripagora.template.dto.request;

import com.tripagora.itinerary.entity.Itineraries;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ItineraryUpdateRequest(
        @Valid
        @NotNull(message = "일정 목록은 필수입니다.")
        List<ItineraryItemRequest> itineraries) {

    // 일차가 1일차부터 이어지지 않으면 여기서 예외가 난다
    public Itineraries toItineraries() {
        return new Itineraries(itineraries.stream()
                .map(ItineraryItemRequest::toItinerary)
                .toList());
    }
}