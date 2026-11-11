package com.example.TripAgora.session.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record SessionDetailResponse(
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
        List<SessionParticipantResponse> participants,
        Long guideProfileId,
        boolean isParticipating,
        boolean isMySession,
        boolean isInWishlist,
        boolean hasWrittenReview,
        Long reviewId) {}