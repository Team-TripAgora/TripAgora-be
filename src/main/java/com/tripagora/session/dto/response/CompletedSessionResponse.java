package com.tripagora.session.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record CompletedSessionResponse(
        Long sessionId,
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