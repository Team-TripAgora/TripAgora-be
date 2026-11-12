package com.tripagora.room.dto.response;

import java.time.LocalDateTime;

public record NoticeResponse(Long noticeId,
                             String title,
                             String content,
                             LocalDateTime createdAt) {}