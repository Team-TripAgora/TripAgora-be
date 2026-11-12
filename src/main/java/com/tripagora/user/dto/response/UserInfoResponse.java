package com.tripagora.user.dto.response;

import java.util.List;

public record UserInfoResponse(String nickname,
                               String role,
                               String profileImageUrl,
                               List<Long> tagIds) {}