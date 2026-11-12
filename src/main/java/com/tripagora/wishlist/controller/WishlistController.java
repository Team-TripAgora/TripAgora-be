package com.tripagora.wishlist.controller;

import com.tripagora.common.code.SuccessCode;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.session.dto.response.SessionListResponse;
import com.tripagora.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;

    @GetMapping
    public ApiResponse<SessionListResponse> getMyWishlist(@AuthenticationPrincipal final long userId,
                                                          @PageableDefault(size = 10) Pageable pageable) {
        SessionListResponse response = wishlistService.getMyWishlist(userId, pageable);
        return ApiResponse.success(SuccessCode.OK, response);
    }

    @PostMapping("/{sessionId}")
    public ApiResponse<Void> addSessionToWishlist(@AuthenticationPrincipal final long userId,
                                                  @PathVariable final long sessionId) {
        wishlistService.addSessionToWishlist(userId, sessionId);
        return ApiResponse.success(SuccessCode.OK);
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> removeSessionFromWishlist(@AuthenticationPrincipal final long userId,
                                                       @PathVariable final long sessionId) {
        wishlistService.removeSessionFromWishlist(userId, sessionId);
        return ApiResponse.success(SuccessCode.OK);
    }
}