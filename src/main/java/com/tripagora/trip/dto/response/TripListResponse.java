package com.tripagora.trip.dto.response;

import java.util.List;

public record TripListResponse(
        List<TripSummaryResponse> sessions,
        boolean hasNext) {}