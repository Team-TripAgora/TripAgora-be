package com.tripagora.room.controller;

import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.room.dto.request.NoticeCreateRequest;
import com.tripagora.room.dto.request.NoticeUpdateRequest;
import com.tripagora.room.dto.response.NoticeListResponse;
import com.tripagora.room.dto.response.NoticeResponse;
import com.tripagora.room.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}/notices")
public class NoticeController {
    private final NoticeService noticeService;

    @PostMapping
    public ApiResponse<NoticeResponse> createNotice(@AuthenticationPrincipal final long userId,
                                                    @PathVariable final long roomId,
                                                    @RequestBody @Valid final NoticeCreateRequest request) {
        NoticeResponse response = noticeService.createNotice(userId, roomId, request);
        return ApiResponse.success(SuccessCode.CREATED, response);
    }

    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeResponse> getNotice(@AuthenticationPrincipal final long userId,
                                                 @PathVariable final long roomId,
                                                 @PathVariable final long noticeId) {
        NoticeResponse response = noticeService.getNotice(userId, roomId, noticeId);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @GetMapping
    public ApiResponse<NoticeListResponse> getNotices(@AuthenticationPrincipal final long userId,
                                                      @PathVariable final long roomId,
                                                      @PageableDefault(size = 10) Pageable pageable) {
        NoticeListResponse response = noticeService.getNotices(userId, roomId, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PatchMapping("/{noticeId}")
    public ApiResponse<NoticeResponse> updateNotice(@AuthenticationPrincipal final long userId,
                                                    @PathVariable final long roomId,
                                                    @PathVariable final long noticeId,
                                                    @RequestBody @Valid final NoticeUpdateRequest request) {
        NoticeResponse response = noticeService.updateNotice(userId, roomId, noticeId, request);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @DeleteMapping("/{noticeId}")
    public ApiResponse<Void> deleteNotice(@AuthenticationPrincipal final long userId,
                                          @PathVariable final long roomId,
                                          @PathVariable final long noticeId) {
        noticeService.deleteNotice(userId, roomId, noticeId);
        return ApiResponse.success(SuccessCode.OK);
    }
}