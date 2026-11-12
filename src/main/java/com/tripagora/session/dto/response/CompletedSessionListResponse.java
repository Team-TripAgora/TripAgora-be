package com.tripagora.session.dto.response;

import java.util.List;

public record CompletedSessionListResponse(
        List<CompletedSessionResponse> sessions,
        boolean hasNext) {}