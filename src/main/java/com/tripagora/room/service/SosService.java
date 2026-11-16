package com.tripagora.room.service;

import com.tripagora.common.service.FcmService;
import com.tripagora.participation.entity.Participation;
import com.tripagora.participation.repository.ParticipationRepository;
import com.tripagora.trip.entity.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SosService {
    private final RoomAccessValidator roomAccessValidator;
    private final ParticipationRepository participationRepository;
    private final FcmService fcmService;

    // 진행 중인 여행의 참가자만 보낼 수 있고, 같은 여행의 다른 참가자에게 간다.
    // DB 조회는 각자 짧게 끝내고 FCM 발송은 트랜잭션 밖에서 한다 (외부 호출 동안 DB 커넥션을 잡지 않는다)
    public void sendSos(Long userId, Long roomId) {
        Participation participation = roomAccessValidator.validateParticipant(userId, roomId);
        Trip trip = participation.getTrip();
        trip.validateSosSendable();

        List<String> targetTokens = participationRepository.findFcmTokensOfOtherParticipants(trip.getId(), userId);

        if (!targetTokens.isEmpty()) {
            String title = "🚨 긴급 SOS 요청";
            String body = String.format("'%s'님이 긴급 도움을 요청했습니다!", participation.getUser().getNickname());

            fcmService.sendMulticastMessage(targetTokens, title, body, trip.getId());
        }
    }
}
