package com.tripagora.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(
        @NotBlank(message = "공백일 수 없습니다.")
        String refreshToken) {}