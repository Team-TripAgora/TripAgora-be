package com.tripagora.trip.dto.response;

public record TripParticipantResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String role) {}