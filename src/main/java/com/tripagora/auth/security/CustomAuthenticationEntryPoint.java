package com.tripagora.auth.security;

import com.tripagora.common.code.AuthErrorCode;
import com.tripagora.common.code.ResponseCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.common.response.ApiResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ResponseCode code = AuthErrorCode.UNAUTHORIZED_ACCESS;
        ApiResponseWriter.write(response, code.getStatusValue(), ApiResponse.fail(code));
    }
}