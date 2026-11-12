package com.tripagora.room.service;

import com.tripagora.auth.exception.AccessDeniedException;
import com.tripagora.participation.entity.Participation;
import com.tripagora.participation.exception.ParticipationNotFoundException;
import com.tripagora.participation.repository.ParticipationRepository;
import com.tripagora.room.entity.Room;
import com.tripagora.room.exception.RoomNotFoundException;
import com.tripagora.room.repository.RoomRepository;
import com.tripagora.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final ParticipationRepository participationRepository;

    protected Room checkRoomAndGuide(long userId, long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        Long tripId = room.getTrip().getId();
        Participation participation = participationRepository.findByUser_IdAndTrip_Id(userId, tripId).orElseThrow(ParticipationNotFoundException::new);

        if (participation.getRole() != Role.GUIDE) {
            throw new AccessDeniedException();
        }

        return room;
    }

    protected Room checkRoomAndParticipant(long userId, long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        Long tripId = room.getTrip().getId();
        participationRepository.findByUser_IdAndTrip_Id(userId, tripId).orElseThrow(ParticipationNotFoundException::new);

        return room;
    }
}