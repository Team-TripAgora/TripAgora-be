package com.tripagora.trip.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record TripDetailResponse(
        Long templateId,
        String title,
        String content,
        List<Long> regionIds,
        List<Long> tagIds,
        List<String> imageUrls,
        Integer maxParticipants,
        Integer currentParticipants,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate,
        String status,
        Long roomId,
        List<TripParticipantResponse> participants,
        Long guideProfileId,
        boolean isParticipating,
        boolean isMyTrip,
        boolean isInWishlist,
        boolean hasWrittenReview,
        Long reviewId) {}