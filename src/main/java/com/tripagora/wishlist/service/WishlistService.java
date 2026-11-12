package com.tripagora.wishlist.service;

import com.tripagora.trip.dto.response.TripListResponse;
import com.tripagora.trip.entity.Trip;
import com.tripagora.trip.exception.TripNotFoundException;
import com.tripagora.trip.repository.TripRepository;
import com.tripagora.trip.service.TripService;
import com.tripagora.user.entity.User;
import com.tripagora.user.exception.UserNotFoundException;
import com.tripagora.user.repository.UserRepository;
import com.tripagora.wishlist.entity.Wishlist;
import com.tripagora.wishlist.exception.AlreadyInWishlistException;
import com.tripagora.wishlist.exception.WishlistNotFoundException;
import com.tripagora.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;

    @Transactional
    public void addTripToWishlist(long userId, long tripId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Trip trip = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new);

        wishlistRepository.findByUserAndTrip(user, trip).ifPresent(like -> {
            throw new AlreadyInWishlistException();
        });

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .trip(trip)
                .build();

        wishlistRepository.save(wishlist);
    }

    @Transactional
    public void removeTripFromWishlist(long userId, long tripId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Trip trip = tripRepository.findById(tripId).orElseThrow(TripNotFoundException::new);

        Wishlist wishlist = wishlistRepository.findByUserAndTrip(user, trip)
                .orElseThrow(WishlistNotFoundException::new);

        wishlistRepository.delete(wishlist);
    }

    @Transactional(readOnly = true)
    public TripListResponse getMyWishlist(long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Slice<Wishlist> wishlistSlice = wishlistRepository.findByUser(user, pageable);
        Slice<Trip> tripSlice = wishlistSlice.map(Wishlist::getTrip);

        return TripService.getTripListResponse(tripSlice);
    }
}