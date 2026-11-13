package com.tripagora.room.controller;

import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.room.service.SosService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SosController {
    private final SosService sosService;

    // SOS 요청 보내기
    @PostMapping("/rooms/{roomId}/sos")
    public ApiResponse<Void> sendSos(@AuthenticationPrincipal final long userId,
                                     @PathVariable Long roomId) {
        sosService.sendSos(userId, roomId);
        return ApiResponse.success(SuccessCode.OK);
    }
}