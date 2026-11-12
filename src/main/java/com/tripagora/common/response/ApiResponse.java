package com.tripagora.common.response;

import com.tripagora.common.code.ResponseCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    private final int code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<FieldErrorResponse> errors;

    public static <T> ApiResponse<T> success(ResponseCode successCode) {
        return new ApiResponse<>(successCode.getStatusValue(), successCode.getMessage(), null, List.of());
    }

    public static <T> ApiResponse<T> success(ResponseCode successCode, T data) {
        return new ApiResponse<>(successCode.getStatusValue(), successCode.getMessage(), data, List.of());
    }

    public static <T> ApiResponse<T> fail(ResponseCode errorCode) {
        return new ApiResponse<>(errorCode.getStatusValue(), errorCode.getMessage(), null, List.of());
    }

    public static <T> ApiResponse<T> fail(ResponseCode errorCode, BindingResult bindingResult) {
        return new ApiResponse<>(errorCode.getStatusValue(), errorCode.getMessage(), null, FieldErrorResponse.of(bindingResult));
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FieldErrorResponse {
        private final String field;
        private final String reason;

        public static List<FieldErrorResponse> of(BindingResult bindingResult) {
            return bindingResult.getFieldErrors().stream()
                    .map(e -> new FieldErrorResponse(e.getField(), e.getDefaultMessage()))
                    .collect(Collectors.toList());
        }
    }
}