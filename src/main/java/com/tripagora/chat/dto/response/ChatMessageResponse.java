package com.tripagora.chat.dto.response;

import com.tripagora.user.entity.Role;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long chatMessageId,
        Long roomId,
        Long senderId,
        String senderNickname,
        String senderImageUrl,
        String content,
        LocalDateTime sentAt,
        Role senderRole) {}