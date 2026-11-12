package com.tripagora.location.repository;

import com.tripagora.location.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {
    Optional<UserLocation> findByUser_IdAndRoom_Id(Long userId, Long roomId);
    List<UserLocation> findByRoom_Id(Long roomId);
}