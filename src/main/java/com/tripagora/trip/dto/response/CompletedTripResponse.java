package com.tripagora.trip.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record CompletedTripResponse(
        Long tripId,
        String title,
        String firstImageUrl,
        List<Long> regionIds,
        Integer maxParticipants,
        Integer currentParticipants,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,
        String status,
        Long roomId,
        boolean hasWrittenReview,
        Long reviewId) {}