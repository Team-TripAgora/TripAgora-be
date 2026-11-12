package com.tripagora.room.dto.response;

import java.util.List;

public record NoticeListResponse(
        List<NoticeSummaryResponse> notices,
        boolean hasNext) {}