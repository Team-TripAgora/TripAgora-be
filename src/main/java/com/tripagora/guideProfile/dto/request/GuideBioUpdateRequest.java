package com.tripagora.guideProfile.dto.request;

import jakarta.validation.constraints.Size;

public record GuideBioUpdateRequest(
        @Size(max = 1000, message = "소개글은 최대 1000자까지 입력 가능합니다.")
        String bio) {}