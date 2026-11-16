package com.tripagora.common;

import com.tripagora.common.exception.GlobalExceptionHandler;
import com.tripagora.common.image.exception.ImageUploadFailedException;
import com.tripagora.trip.exception.TripNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

// 예외 로그: 정상적인 거절(4xx)은 warn 한 줄(스택트레이스 없이), 서버 문제(5xx)만 error + 스택트레이스
@ExtendWith(OutputCaptureExtension.class)
class ExceptionLogTest {
    private static final String STACK_TRACE_LINE = "\tat ";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("업무 거절(4xx)은 warn 한 줄로 남기고 스택트레이스는 남기지 않는다")
    void businessRejectionLogsOneLine(CapturedOutput output) {
        handler.handleCustomException(new TripNotFoundException());

        assertThat(output.getOut()).contains("WARN").contains("여행을 찾을 수 없습니다.");
        assertThat(output.getOut()).doesNotContain(STACK_TRACE_LINE);
    }

    @Test
    @DisplayName("입력 검증 오류(4xx)는 어느 칸이 왜 틀렸는지 한 줄로 남기고 스택트레이스는 남기지 않는다")
    void validationErrorLogsOneLine(CapturedOutput output) {
        BindException e = new BindException(new Object(), "request");
        e.addError(new FieldError("request", "title", "제목은 필수입니다."));

        handler.handleBindException(e);

        assertThat(output.getOut()).contains("WARN").contains("title").contains("제목은 필수입니다.");
        assertThat(output.getOut()).doesNotContain(STACK_TRACE_LINE);
    }

    @Test
    @DisplayName("서버 문제인 업무 오류(5xx)는 error와 스택트레이스로 남긴다")
    void serverSideBusinessErrorLogsStackTrace(CapturedOutput output) {
        handler.handleCustomException(new ImageUploadFailedException());

        assertThat(output.getOut()).contains("ERROR").contains("이미지 업로드에 실패했습니다.").contains(STACK_TRACE_LINE);
    }

    @Test
    @DisplayName("예상 못 한 오류는 error와 스택트레이스로 남긴다")
    void unexpectedErrorLogsStackTrace(CapturedOutput output) {
        handler.handleException(new IllegalStateException("예상 못 한 상태"));

        assertThat(output.getOut()).contains("ERROR").contains("예상 못 한 상태").contains(STACK_TRACE_LINE);
    }
}
