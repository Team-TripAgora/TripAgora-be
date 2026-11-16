package com.tripagora.chat.service;

import com.tripagora.chat.dto.request.ChatMessageRequest;
import com.tripagora.chat.dto.response.ChatHistoryResponse;
import com.tripagora.chat.dto.response.ChatMessageResponse;
import com.tripagora.chat.entity.ChatMessage;
import com.tripagora.chat.repository.ChatMessageRepository;
import com.tripagora.participation.entity.Participation;
import com.tripagora.room.entity.Room;
import com.tripagora.room.service.RoomAccessValidator;
import com.tripagora.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final RoomAccessValidator roomAccessValidator;
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessageResponse processAndSaveMessage(Long userId, Long roomId, ChatMessageRequest request) {
        Participation participation = roomAccessValidator.validateParticipant(userId, roomId);

        User sender = participation.getUser();
        Room room = participation.getTrip().getRoom();

        ChatMessage chatMessage = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(request.content())
                .senderRole(participation.getRole())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        return new ChatMessageResponse(
                savedMessage.getId(),
                roomId,
                sender.getId(),
                sender.getNickname(),
                sender.getImageUrl(),
                savedMessage.getContent(),
                savedMessage.getCreatedAt(),
                savedMessage.getSenderRole()
        );
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(Long userId, Long roomId, Pageable pageable) {
        roomAccessValidator.validateParticipant(userId, roomId);

        Slice<ChatMessage> messages = chatMessageRepository.findByRoom_Id(roomId, pageable);

        List<ChatMessageResponse> chatMessages = messages.getContent().stream()
                .map(msg -> new ChatMessageResponse(
                        msg.getId(),
                        msg.getRoom().getId(),
                        msg.getSender().getId(),
                        msg.getSender().getNickname(),
                        msg.getSender().getImageUrl(),
                        msg.getContent(),
                        msg.getCreatedAt(),
                        msg.getSenderRole()
                ))
                .toList();

        return new ChatHistoryResponse(chatMessages, messages.hasNext());
    }
}