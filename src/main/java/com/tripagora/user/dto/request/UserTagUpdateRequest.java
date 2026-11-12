package com.tripagora.user.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserTagUpdateRequest(
        @NotEmpty(message = "태그는 비어있을 수 없습니다.")
        @Size(min = 3, message = "최소 3개 이상의 태그를 선택해야 합니다.")
        List<Long> tagIds) {}