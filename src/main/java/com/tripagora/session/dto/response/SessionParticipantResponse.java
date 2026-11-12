package com.tripagora.session.dto.response;

public record SessionParticipantResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String role) {}