package com.tripagora.chat.controller;

import com.tripagora.chat.dto.response.ChatHistoryResponse;
import com.tripagora.chat.service.ChatService;
import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    @GetMapping("/room/{roomId}/messages")
    public ApiResponse<ChatHistoryResponse> getChatMessages(@AuthenticationPrincipal final long userId,
                                                            @PathVariable Long roomId,
                                                            @PageableDefault(size = 30, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        ChatHistoryResponse response = chatService.getChatHistory(userId, roomId, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }
}