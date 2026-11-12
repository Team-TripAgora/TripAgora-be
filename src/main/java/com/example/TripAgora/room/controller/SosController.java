package com.example.TripAgora.room.controller;

import com.example.TripAgora.common.code.SuccessCode;
import com.example.TripAgora.common.response.ApiResponse;
import com.example.TripAgora.room.dto.request.FcmTokenRequest;
import com.example.TripAgora.room.service.SosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SosController {
    private final SosService sosService;

    // FCM 토큰 갱신
    @PostMapping("/users/me/fcm-token")
    public ApiResponse<Void> updateFcmToken(@AuthenticationPrincipal final long userId,
                                            @Valid @RequestBody FcmTokenRequest request) {
        sosService.updateFcmToken(userId, request.token());
        return ApiResponse.success(SuccessCode.OK);
    }

    // SOS 요청 보내기
    @PostMapping("/rooms/{roomId}/sos")
    public ApiResponse<Void> sendSos(@AuthenticationPrincipal final long userId,
                                     @PathVariable Long roomId) {
        sosService.sendSos(userId, roomId);
        return ApiResponse.success(SuccessCode.OK);
    }
}