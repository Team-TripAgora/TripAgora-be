package com.example.TripAgora.wishlist.service;

import com.example.TripAgora.session.dto.response.SessionListResponse;
import com.example.TripAgora.session.entity.Session;
import com.example.TripAgora.session.exception.SessionNotFoundException;
import com.example.TripAgora.session.repository.SessionRepository;
import com.example.TripAgora.session.service.SessionService;
import com.example.TripAgora.user.entity.User;
import com.example.TripAgora.user.exception.UserNotFoundException;
import com.example.TripAgora.user.repository.UserRepository;
import com.example.TripAgora.wishlist.entity.Wishlist;
import com.example.TripAgora.wishlist.exception.AlreadyInWishlistException;
import com.example.TripAgora.wishlist.exception.WishlistNotFoundException;
import com.example.TripAgora.wishlist.repository.WishlistRepository;
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
    private final SessionRepository sessionRepository;

    @Transactional
    public void addSessionToWishlist(long userId, long sessionId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Session session = sessionRepository.findById(sessionId).orElseThrow(SessionNotFoundException::new);

        wishlistRepository.findByUserAndSession(user, session).ifPresent(like -> {
            throw new AlreadyInWishlistException();
        });

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .session(session)
                .build();

        wishlistRepository.save(wishlist);
    }

    @Transactional
    public void removeSessionFromWishlist(long userId, long sessionId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Session session = sessionRepository.findById(sessionId).orElseThrow(SessionNotFoundException::new);

        Wishlist wishlist = wishlistRepository.findByUserAndSession(user, session)
                .orElseThrow(WishlistNotFoundException::new);

        wishlistRepository.delete(wishlist);
    }

    @Transactional(readOnly = true)
    public SessionListResponse getMyWishlist(long userId, Pageable pageable) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        Slice<Wishlist> wishlistSlice = wishlistRepository.findByUser(user, pageable);
        Slice<Session> sessionSlice = wishlistSlice.map(Wishlist::getSession);

        return SessionService.getSessionListResponse(sessionSlice);
    }
}