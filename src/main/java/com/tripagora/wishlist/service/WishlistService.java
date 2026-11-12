package com.tripagora.wishlist.service;

import com.tripagora.session.dto.response.SessionListResponse;
import com.tripagora.session.entity.Session;
import com.tripagora.session.exception.SessionNotFoundException;
import com.tripagora.session.repository.SessionRepository;
import com.tripagora.session.service.SessionService;
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