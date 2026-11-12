package com.tripagora.template.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TemplateTagUpdateRequest(
        @NotNull(message = "태그 목록은 필수입니다.")
        @Size(max = 5, message = "최대 5개까지 태그를 선택할 수 있습니다.")
        List<Long> tagIds) {}