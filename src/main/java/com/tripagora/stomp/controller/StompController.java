package com.tripagora.stomp.controller;

import com.tripagora.chat.dto.request.ChatMessageRequest;
import com.tripagora.chat.dto.response.ChatMessageResponse;
import com.tripagora.chat.service.ChatService;
import com.tripagora.location.dto.request.LocationUpdateRequest;
import com.tripagora.location.dto.response.LocationResponse;
import com.tripagora.location.service.LocationService;
import com.tripagora.stomp.RoomDestinations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
                            ChatMessageRequest request,
                            Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        ChatMessageResponse response = chatService.processAndSaveMessage(userId, roomId, request);
        messagingTemplate.convertAndSend(RoomDestinations.topic(roomId, RoomDestinations.CHAT), response);
    }

    @MessageMapping("/room/{roomId}/" + RoomDestinations.LOCATION)
    public void sendLocation(@DestinationVariable(value = "roomId") Long roomId,
                             LocationUpdateRequest request,
                             Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        LocationResponse response = locationService.updateLocation(userId, roomId, request);
        messagingTemplate.convertAndSend(RoomDestinations.topic(roomId, RoomDestinations.LOCATION), response);
    }
}