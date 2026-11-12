package com.tripagora.template.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ItineraryUpdateRequest(
        @Valid
        @NotNull(message = "일정 목록은 필수입니다.")
        List<ItineraryItemRequest> itineraries) {}