package com.tripagora.user.dto.response;

public record TravelerSwitchResponse(String accessToken, String refreshToken, Long userId) {}