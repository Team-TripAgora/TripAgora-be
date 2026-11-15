package com.tripagora.auth.security;

import com.tripagora.common.code.AuthErrorCode;
import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.common.response.ApiResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 역할 검사(hasRole)에서 막히면 다른 오류와 같은 공통 응답 형식으로 403을 준다
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ResponseCode code = AuthErrorCode.ACCESS_DENIED;
        ApiResponseWriter.write(response, code.getStatusValue(), ApiResponse.fail(code));
    }
}
