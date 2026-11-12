package com.tripagora.template.dto.response;

import java.util.List;

public record TemplateDetailResponse(String title,
                                     String content,
                                     List<Long> regionIds,
                                     List<Long> tagIds,
                                     List<String> imageUrls) {}