package com.tripagora.auth.dto;

public record SocialLoginResponse(String accessToken, String refreshToken, boolean isNewUser) {}