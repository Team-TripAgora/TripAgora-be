package com.tripagora.tag.repository;

import com.tripagora.tag.entity.UserTag;
import com.tripagora.tag.entity.UserTagId;
import com.tripagora.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTagRepository extends JpaRepository<UserTag, UserTagId> {
    void deleteByUser(User user);
}