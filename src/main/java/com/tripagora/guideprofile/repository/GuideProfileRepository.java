package com.tripagora.guideprofile.repository;

import com.tripagora.guideprofile.entity.GuideProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuideProfileRepository extends JpaRepository<GuideProfile, Long> {
    Optional<GuideProfile> findByUser_Id(Long userId);
}