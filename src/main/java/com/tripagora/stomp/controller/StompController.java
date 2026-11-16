package com.tripagora.stomp.controller;

import com.tripagora.chat.dto.request.ChatMessageRequest;
import com.tripagora.chat.dto.response.ChatMessageResponse;
import com.tripagora.chat.service.ChatService;
import com.tripagora.location.dto.request.LocationUpdateRequest;
import com.tripagora.location.dto.response.LocationResponse;
import com.tripagora.common.code.CommonErrorCode;
import com.tripagora.common.exception.ApplicationException;
import com.tripagora.common.response.ApiResponse;
import com.tripagora.location.service.LocationService;
import com.tripagora.stomp.RoomDestinations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StompController {
    private final ChatService chatService;
    private final LocationService locationService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{roomId}/" + RoomDestinations.CHAT)
    public void sendMessage(@DestinationVariable(value = "roomId") Long roomId,
                            @Valid @Payload ChatMessageRequest request,
                            Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        ChatMessageResponse response = chatService.processAndSaveMessage(userId, roomId, request);
        messagingTemplate.convertAndSend(RoomDestinations.topic(roomId, RoomDestinations.CHAT), response);
    }

    @MessageMapping("/room/{roomId}/" + RoomDestinations.LOCATION)
    public void sendLocation(@DestinationVariable(value = "roomId") Long roomId,
                             @Valid @Payload LocationUpdateRequest request,
                             Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        LocationResponse response = locationService.updateLocation(userId, roomId, request);
        messagingTemplate.convertAndSend(RoomDestinations.topic(roomId, RoomDestinations.LOCATION), response);
    }

    // 메시지 처리 중 오류는 보낸 사람(그 연결)에게만 HTTP API와 같은 오류 응답 형식으로 알린다
    @MessageExceptionHandler
    @SendToUser(destinations = RoomDestinations.ERRORS, broadcast = false)
    public ApiResponse<?> handleApplicationException(ApplicationException e) {
        log.warn("[STOMP ApplicationException] : {}", e.getMessage());
        return ApiResponse.fail(e.getErrorCode());
    }

    @MessageExceptionHandler
    @SendToUser(destinations = RoomDestinations.ERRORS, broadcast = false)
    public ApiResponse<?> handleInvalidMessage(MethodArgumentNotValidException e) {
        log.warn("[STOMP MethodArgumentNotValidException] : {}", e.getMessage());
        return e.getBindingResult() == null
                ? ApiResponse.fail(CommonErrorCode.INVALID_INPUT_VALUE)
                : ApiResponse.fail(CommonErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
    }

    @MessageExceptionHandler
    @SendToUser(destinations = RoomDestinations.ERRORS, broadcast = false)
    public ApiResponse<?> handleUnreadableMessage(MessageConversionException e) {
        log.warn("[STOMP MessageConversionException] : {}", e.getMessage());
        return ApiResponse.fail(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    @MessageExceptionHandler
    @SendToUser(destinations = RoomDestinations.ERRORS, broadcast = false)
    public ApiResponse<?> handleException(Exception e) {
        log.error("[STOMP Exception]", e);
        return ApiResponse.fail(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
}