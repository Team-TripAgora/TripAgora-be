package com.tripagora.itinerary.dto.response;

import java.util.List;

public record ItinerariesResponse(List<ItineraryItemResponse> itineraries) {}