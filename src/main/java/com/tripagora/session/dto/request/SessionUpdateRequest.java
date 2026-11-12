package com.tripagora.session.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SessionUpdateRequest(
        @NotNull(message = "모집인원은 필수입니다.")
        @Min(value = 1, message = "모집인원은 1명 이상이어야 합니다.")
        Integer maxParticipants,

        @NotNull(message = "여행 시작일은 필수입니다.")
        @FutureOrPresent(message = "여행 시작일은 오늘 이후여야 합니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate) {}