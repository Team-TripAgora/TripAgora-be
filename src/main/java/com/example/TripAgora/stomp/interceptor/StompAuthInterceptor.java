package com.example.TripAgora.stomp.interceptor;

import com.example.TripAgora.auth.exception.AccessTokenInvalidException;
import com.example.TripAgora.auth.exception.UnAuthorizedAccessException;
import com.example.TripAgora.auth.util.JWTUtil;
import com.example.TripAgora.stomp.exception.InvalidDestinationException;
import com.example.TripAgora.participation.entity.Participation;
import com.example.TripAgora.participation.exception.ParticipationNotFoundException;
import com.example.TripAgora.participation.repository.ParticipationRepository;
import com.example.TripAgora.room.entity.Room;
import com.example.TripAgora.room.exception.RoomNotFoundException;
import com.example.TripAgora.room.repository.RoomRepository;
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
    private final RoomRepository roomRepository;
    private final ParticipationRepository participationRepository;

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

            if (!jwtUtil.isValid(accessToken, true)) {
                throw new AccessTokenInvalidException();
            }

            Long userId = jwtUtil.getUserId(accessToken);
            String role = jwtUtil.getRole(accessToken);

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
            if (destination == null || !destination.startsWith("/topic/room/")) {
                log.warn("STOMP SUBSCRIBE 실패 - 유효하지 않은 destination: {}", destination);
                throw new InvalidDestinationException();
            }

            String[] parts = destination.split("/");

            Long userId = (Long) auth.getPrincipal();
            Long roomId = Long.parseLong(parts[3]);
            Room room = roomRepository.findById(roomId).orElseThrow(RoomNotFoundException::new);
            Long sessionId = room.getSession().getId();

            Participation participation = participationRepository.findByUser_IdAndSession_Id(userId, sessionId)
                    .orElseThrow(ParticipationNotFoundException::new);

            log.info("STOMP SUBSCRIBE 성공 - userId: {}, roomId: {}", userId, roomId);
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            Authentication auth = (Authentication) accessor.getUser();
            if (auth == null || auth.getPrincipal() == null) {
                log.warn("STOMP SEND 실패 - 인증 정보(auth)가 존재하지 않습니다.");
                throw new UnAuthorizedAccessException();
            }

            String destination = accessor.getDestination();
            if (destination == null || !destination.startsWith("/publish/room/")) {
                log.warn("STOMP SEND 실패 - 유효하지 않은 destination: {}", destination);
                throw new InvalidDestinationException();
            }

            Long userId = (Long) auth.getPrincipal();
            log.info("STOMP SEND 성공 - userId: {}", userId);
        }

        return message;
    }
}