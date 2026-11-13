package com.tripagora.user.repository;

import com.tripagora.user.entity.SocialType;
import com.tripagora.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySocialIdAndSocialType(String socialId, SocialType type);
    boolean existsByNicknameAndIdNot(String nickname, Long id);
    List<User> findAllByFcmTokenAndIdNot(String fcmToken, Long id);
}