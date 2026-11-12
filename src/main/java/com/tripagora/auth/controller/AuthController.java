package com.tripagora.auth.controller;

import com.tripagora.auth.dto.ReissueRequest;
import com.tripagora.auth.dto.ReissueResponse;
import com.tripagora.auth.dto.SocialLoginRequest;
import com.tripagora.auth.dto.SocialLoginResponse;
import com.tripagora.auth.service.JWTService;
import com.tripagora.auth.service.LoginService;
import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.user.entity.SocialType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final LoginService loginService;
    private final JWTService jwtService;

    @PostMapping("/login/{socialType}")
    public ApiResponse<SocialLoginResponse> socialLogin(@PathVariable final SocialType socialType,
                                                        @RequestBody @Valid final SocialLoginRequest request) {
        SocialLoginResponse response = loginService.socialLogin(request, socialType);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal final long userId) {
        loginService.logout(userId);
        return ApiResponse.success(SuccessCode.NO_CONTENT);
    }

    @PostMapping("/reissue")
    public ApiResponse<ReissueResponse> reissue(@RequestBody @Valid final ReissueRequest request) {
        ReissueResponse response = jwtService.reissue(request);
        return ApiResponse.success(SuccessCode.CREATED, response);
    }
}