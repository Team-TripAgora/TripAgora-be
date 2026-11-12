package com.tripagora.trip.dto.response;

import java.util.List;

public record CompletedTripListResponse(
        List<CompletedTripResponse> sessions,
        boolean hasNext) {}