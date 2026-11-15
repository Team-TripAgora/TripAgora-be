package com.tripagora.user.repository;

import com.tripagora.user.entity.Role;
import com.tripagora.user.entity.SocialType;
import com.tripagora.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySocialIdAndSocialType(String socialId, SocialType type);

    // 같은 사용자의 참가 신청을 줄 세운다 (중복 신청, 날짜가 겹치는 여행 동시 신청)
    // 비관적 락(SELECT ... FOR UPDATE). 트랜잭션의 첫 읽기로 잡아야 한다: 먼저 다른 것을 읽으면 그때 잡힌
    // REPEATABLE READ 스냅샷 때문에 락을 얻은 뒤의 조회가 앞 트랜잭션이 커밋한 결과를 보지 못한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

    boolean existsByNicknameAndIdNot(String nickname, Long id);
    List<User> findAllByFcmTokenAndIdNot(String fcmToken, Long id);

    // 현재 역할만 읽는다 (가이드 토큰이 아직 유효한 역할인지 확인할 때)
    @Query("SELECT u.role FROM User u WHERE u.id = :userId")
    Optional<Role> findRoleById(@Param("userId") Long userId);
}