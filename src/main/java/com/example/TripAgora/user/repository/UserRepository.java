package com.example.TripAgora.user.repository;

import com.example.TripAgora.user.entity.SocialType;
import com.example.TripAgora.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySocialIdAndSocialType(String socialId, SocialType type);
    boolean existsByNicknameAndIdNot(String nickname, Long id);
}