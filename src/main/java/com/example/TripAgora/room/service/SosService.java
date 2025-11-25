package com.example.TripAgora.room.service;

import com.example.TripAgora.common.service.FcmService;
import com.example.TripAgora.participation.entity.Participation;
import com.example.TripAgora.room.entity.Room;
import com.example.TripAgora.room.exception.RoomNotFoundException;
import com.example.TripAgora.room.repository.RoomRepository;
import com.example.TripAgora.user.entity.User;
import com.example.TripAgora.user.exception.UserNotFoundException;
import com.example.TripAgora.user.repository.UserRepository;
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

    // 1. FCM 토큰 저장/갱신 (로그인 시 호출)
    @Transactional
    public void updateFcmToken(Long userId, String token) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        user.updateFcmToken(token);
    }

    @Transactional(readOnly = true)
    public void sendSos(Long userId, Long roomId) {
        User sender = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);

        List<String> targetTokens = room.getSession().getParticipants().stream()
                .map(Participation::getUser)
                .filter(user -> !Objects.equals(user.getId(), userId)) // 보낸 사람(나) 제외
                .map(User::getFcmToken)
                .filter(StringUtils::hasText) // 토큰이 null이거나 빈 문자열인 경우 제외
                .toList();

        if (!targetTokens.isEmpty()) {
            String title = "🚨 긴급 SOS 요청";
            String body = String.format("'%s'님이 긴급 도움을 요청했습니다!", sender.getNickname());

            fcmService.sendMulticastMessage(targetTokens, title, body);
        }
    }
}