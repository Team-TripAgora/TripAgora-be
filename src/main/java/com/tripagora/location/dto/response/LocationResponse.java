package com.tripagora.location.dto.response;

import java.time.LocalDateTime;

public record LocationResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        Double latitude,
        Double longitude,
        LocalDateTime updatedAt) {}