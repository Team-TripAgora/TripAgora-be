package com.tripagora.room.service;

import com.tripagora.auth.exception.AccessDeniedException;
import com.tripagora.common.exception.ApplicationException;
import com.tripagora.participation.entity.Participation;
import com.tripagora.participation.repository.ParticipationRepository;
import com.tripagora.room.exception.RoomNotFoundException;
import com.tripagora.room.repository.RoomRepository;
import com.tripagora.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 채팅방(채팅·위치·공지·구독)에 들어올 수 있는지 한 곳에서 확인한다.
// 채팅방 → 여행 → 참가를 따로 조회하지 않고, 채팅방 id와 사용자 id로 참가를 JOIN 한 번에 읽는다
@Component
@RequiredArgsConstructor
public class RoomAccessValidator {
    private final ParticipationRepository participationRepository;
    private final RoomRepository roomRepository;

    // 그 채팅방 여행의 참가자인지 확인하고, 참가(사용자·여행 포함)를 돌려준다
    public Participation validateParticipant(long userId, long roomId) {
        return participationRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> accessFailure(roomId));
    }

    // 그 채팅방 여행의 가이드인지 확인한다 (공지 작성·수정·삭제)
    public Participation validateGuide(long userId, long roomId) {
        Participation participation = validateParticipant(userId, roomId);
        if (participation.getRole() != Role.GUIDE) {
            throw new AccessDeniedException();
        }
        return participation;
    }

    // 참가가 없을 때만 채팅방이 있는지 본다: 있으면 참가자가 아닌 것(403), 없으면 404
    private ApplicationException accessFailure(long roomId) {
        return roomRepository.existsById(roomId) ? new AccessDeniedException() : new RoomNotFoundException();
    }
}
