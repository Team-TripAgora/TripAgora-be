package com.tripagora.location.service;

import com.tripagora.location.dto.request.LocationUpdateRequest;
import com.tripagora.location.dto.response.LocationResponse;
import com.tripagora.location.entity.UserLocation;
import com.tripagora.location.repository.UserLocationRepository;
import com.tripagora.participation.entity.Participation;
import com.tripagora.participation.exception.ParticipationNotFoundException;
import com.tripagora.participation.repository.ParticipationRepository;
import com.tripagora.room.entity.Room;
import com.tripagora.room.exception.RoomNotFoundException;
import com.tripagora.room.repository.RoomRepository;
import com.tripagora.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {
    private final UserLocationRepository userLocationRepository;
    private final RoomRepository roomRepository;
    private final ParticipationRepository participationRepository;

    @Transactional
    public LocationResponse updateLocation(Long userId, Long roomId, LocationUpdateRequest request) {
        Participation participation = checkAndGetParticipation(userId, roomId);

        User user = participation.getUser();
        Room room = participation.getTrip().getRoom();

        UserLocation userLocation = userLocationRepository.findByUser_IdAndRoom_Id(userId, roomId)
                .orElseGet(() -> UserLocation.builder()
                        .user(user)
                        .room(room)
                        .latitude(request.latitude())
                        .longitude(request.longitude())
                        .build());

        if (userLocation.getId() != null) {
            userLocation.updateLocation(request.latitude(), request.longitude());
        } else {
            userLocationRepository.save(userLocation);
        }

        return new LocationResponse(
                userId,
                user.getNickname(),
                user.getImageUrl(),
                userLocation.getLatitude(),
                userLocation.getLongitude(),
                userLocation.getModifiedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getRoomLocations(Long userId, Long roomId) {
        checkAndGetParticipation(userId, roomId);

        return userLocationRepository.findByRoom_Id(roomId).stream()
                .map(loc -> new LocationResponse(
                        loc.getUser().getId(),
                        loc.getUser().getNickname(),
                        loc.getUser().getImageUrl(),
                        loc.getLatitude(),
                        loc.getLongitude(),
                        loc.getModifiedAt()
                ))
                .toList();
    }

    private Participation checkAndGetParticipation(Long userId, Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
        Long tripId = room.getTrip().getId();

        return participationRepository.findByUser_IdAndTrip_Id(userId, tripId)
                .orElseThrow(ParticipationNotFoundException::new);
    }
}