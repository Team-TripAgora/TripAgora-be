package com.example.TripAgora.template.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TemplateRegionUpdateRequest(
        @NotNull(message = "지역 목록은 필수입니다.")
        List<Long> regionIds) {}