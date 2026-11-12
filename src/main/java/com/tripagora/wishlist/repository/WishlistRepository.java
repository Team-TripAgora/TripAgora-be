package com.tripagora.wishlist.repository;

import com.tripagora.trip.entity.Trip;
import com.tripagora.user.entity.User;
import com.tripagora.wishlist.entity.Wishlist;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByUserAndTrip(User user, Trip trip);
    boolean existsByUser_IdAndTrip_Id(Long userId, Long tripId);
    Slice<Wishlist> findByUser(User user, Pageable pageable);
}