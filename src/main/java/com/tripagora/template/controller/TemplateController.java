package com.tripagora.template.controller;

import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.itinerary.dto.request.ItineraryUpdateRequest;
import com.tripagora.itinerary.dto.response.ItinerariesResponse;
import com.tripagora.template.dto.request.*;
import com.tripagora.template.dto.response.*;
import com.tripagora.template.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateService templateService;

    @GetMapping("/{templateId}")
    public ApiResponse<TemplateDetailResponse> getTemplate(@AuthenticationPrincipal final long userId,
                                                           @PathVariable final long templateId) {
        TemplateDetailResponse response = templateService.getTemplate(userId, templateId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping("/{templateId}/itineraries")
    public ApiResponse<ItinerariesResponse> getItineraries(@AuthenticationPrincipal final long userId,
                                                           @PathVariable final long templateId) {
        ItinerariesResponse response = templateService.getItineraries(userId, templateId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping("/me")
    public ApiResponse<TemplateListResponse> getMyTemplates(@AuthenticationPrincipal final long userId,
                                                            @PageableDefault(size = 10) Pageable pageable) {
        TemplateListResponse response = templateService.getMyTemplates(userId, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping
    public ApiResponse<TemplateCreateResponse> createDraftTemplate(@AuthenticationPrincipal final long userId) {
        TemplateCreateResponse response = templateService.createDraftTemplate(userId);
        return ApiResponse.success(SuccessCode.CREATED, response);
    }

    @PatchMapping("/{templateId}/title")
    public ApiResponse<TemplateTitleUpdateResponse> updateTitle(@AuthenticationPrincipal final long userId,
                                                                @PathVariable final long templateId,
                                                                @RequestBody @Valid final TemplateTitleUpdateRequest request) {
        TemplateTitleUpdateResponse response = templateService.updateTitle(userId, templateId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PatchMapping("/{templateId}/content")
    public ApiResponse<TemplateContentUpdateResponse> updateContent(@AuthenticationPrincipal final long userId,
                                                                    @PathVariable final long templateId,
                                                                    @RequestBody @Valid final TemplateContentUpdateRequest request) {
        TemplateContentUpdateResponse response = templateService.updateContent(userId, templateId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PatchMapping("/{templateId}/images")
    public ApiResponse<TemplateImageUpdateResponse> updateImages(@AuthenticationPrincipal final long userId,
                                                                 @PathVariable final long templateId,
                                                                 @RequestPart("images") final List<MultipartFile> images) {
        TemplateImageUpdateResponse response = templateService.updateImages(userId, templateId, images);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PatchMapping("/{templateId}/tags")
    public ApiResponse<TemplateTagUpdateResponse> updateTags(@AuthenticationPrincipal final long userId,
                                                             @PathVariable final long templateId,
                                                             @RequestBody @Valid final TemplateTagUpdateRequest request) {
        TemplateTagUpdateResponse response = templateService.updateTags(userId, templateId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PatchMapping("/{templateId}/regions")
    public ApiResponse<TemplateRegionUpdateResponse> updateRegions(@AuthenticationPrincipal final long userId,
                                                                   @PathVariable final long templateId,
                                                                   @RequestBody @Valid final TemplateRegionUpdateRequest request) {
        TemplateRegionUpdateResponse response = templateService.updateRegions(userId, templateId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PutMapping("/{templateId}/itineraries")
    public ApiResponse<Void> updateItineraries(@AuthenticationPrincipal final long userId,
                                               @PathVariable final long templateId,
                                               @RequestBody @Valid final ItineraryUpdateRequest request) {
        templateService.updateItineraries(userId, templateId, request);
        return ApiResponse.success(SuccessCode.OK);
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> deleteTemplate(@AuthenticationPrincipal final long userId,
                                            @PathVariable final long templateId) {
        templateService.deleteTemplate(userId, templateId);
        return ApiResponse.success(SuccessCode.OK);
    }
}