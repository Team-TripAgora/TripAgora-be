package com.tripagora.stomp.interceptor;

import com.tripagora.auth.exception.AccessTokenInvalidException;
import com.tripagora.auth.exception.UnAuthorizedAccessException;
import com.tripagora.auth.util.JWTUtil;
import com.tripagora.auth.util.TokenClaims;
import com.tripagora.auth.util.TokenType;
import com.tripagora.stomp.RoomDestinations;
import com.tripagora.stomp.exception.InvalidDestinationException;
import com.tripagora.room.service.RoomAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {
    private final JWTUtil jwtUtil;
    private final RoomAccessValidator roomAccessValidator;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("STOMP CONNECT 실패 - Authorization 헤더가 없거나 올바르지 않음.");
                throw new UnAuthorizedAccessException();
            }

            String accessToken = authHeader.substring(7);

            TokenClaims claims = jwtUtil.parse(accessToken, TokenType.ACCESS)
                    .orElseThrow(AccessTokenInvalidException::new);
            Long userId = claims.userId();
            String role = claims.role();

            List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
            Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            accessor.setUser(auth);

            log.info("STOMP CONNECT 성공 - userId: {}, role: {}", userId, role);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Authentication auth = (Authentication) accessor.getUser();
            if (auth == null || auth.getPrincipal() == null) {
                log.warn("STOMP SUBSCRIBE 실패 - 인증 정보(auth)가 존재하지 않습니다.");
                throw new UnAuthorizedAccessException();
            }

            String destination = accessor.getDestination();
            Long roomId = RoomDestinations.roomIdOfSubscription(destination)
                    .orElseThrow(() -> invalidDestination(StompCommand.SUBSCRIBE, destination));

            Long userId = (Long) auth.getPrincipal();
            roomAccessValidator.validateParticipant(userId, roomId);

            log.info("STOMP SUBSCRIBE 성공 - userId: {}, roomId: {}", userId, roomId);
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            Authentication auth = (Authentication) accessor.getUser();
            if (auth == null || auth.getPrincipal() == null) {
                log.warn("STOMP SEND 실패 - 인증 정보(auth)가 존재하지 않습니다.");
                throw new UnAuthorizedAccessException();
            }

            String destination = accessor.getDestination();
            RoomDestinations.roomIdOfPublish(destination)
                    .orElseThrow(() -> invalidDestination(StompCommand.SEND, destination));

            Long userId = (Long) auth.getPrincipal();
            log.info("STOMP SEND 성공 - userId: {}", userId);
        }

        return message;
    }

    private InvalidDestinationException invalidDestination(StompCommand command, String destination) {
        log.warn("STOMP {} 실패 - 유효하지 않은 destination: {}", command, destination);
        return new InvalidDestinationException();
    }
}