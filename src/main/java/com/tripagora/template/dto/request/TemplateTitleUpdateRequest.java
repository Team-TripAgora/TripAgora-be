package com.tripagora.template.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemplateTitleUpdateRequest(
        @NotBlank(message = "제목은 공백일 수 없습니다.")
        @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
        String title) {}