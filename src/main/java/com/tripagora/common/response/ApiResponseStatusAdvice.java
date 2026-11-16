package com.tripagora.common.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

// 컨트롤러가 ApiResponse를 그대로 반환하면 HTTP 상태가 항상 200이 된다.
// 응답을 쓰기 직전 한 곳에서 본문 code로 HTTP 상태를 정해, 둘이 어긋나지 않게 한다 (오류 응답은 이미 ResponseEntity로 같은 값을 준다)
@ControllerAdvice
public class ApiResponseStatusAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse<?> apiResponse) {
            response.setStatusCode(HttpStatusCode.valueOf(apiResponse.getCode()));
        }
        return body;
    }
}
