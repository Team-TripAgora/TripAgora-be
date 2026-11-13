package com.tripagora.room.service;

import com.tripagora.common.service.FcmService;
import com.tripagora.participation.entity.Participation;
import com.tripagora.room.entity.Room;
import com.tripagora.room.exception.RoomNotFoundException;
import com.tripagora.room.repository.RoomRepository;
import com.tripagora.user.entity.User;
import com.tripagora.user.exception.UserNotFoundException;
import com.tripagora.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SosService {
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final FcmService fcmService;

    @Transactional(readOnly = true)
    public void sendSos(Long userId, Long roomId) {
        User sender = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);

        List<String> targetTokens = room.getTrip().getParticipants().stream()
                .map(Participation::getUser)
                .filter(user -> !Objects.equals(user.getId(), userId)) // 보낸 사람(나) 제외
                .map(User::getFcmToken)
                .filter(StringUtils::hasText) // 토큰이 null이거나 빈 문자열인 경우 제외
                .toList();

        if (!targetTokens.isEmpty()) {
            String title = "🚨 긴급 SOS 요청";
            String body = String.format("'%s'님이 긴급 도움을 요청했습니다!", sender.getNickname());
            Long tripId = room.getTrip().getId();

            fcmService.sendMulticastMessage(targetTokens, title, body, tripId);
        }
    }
}