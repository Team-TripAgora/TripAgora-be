package com.tripagora.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(
        @NotBlank(message = "공백일 수 없습니다.")
        String socialAccessToken) {}