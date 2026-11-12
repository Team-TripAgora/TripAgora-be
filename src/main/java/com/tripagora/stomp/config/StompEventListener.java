package com.tripagora.stomp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class StompEventListener {
    private final Set<String> websocketSessions = ConcurrentHashMap.newKeySet();

    @EventListener
    public void connectHandle(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        websocketSessions.add(accessor.getSessionId());
        log.info("Connected session Id : {}", accessor.getSessionId());
        log.info("Total sessions : {}", websocketSessions.size());
    }

    @EventListener
    public void disconnectHandle(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        websocketSessions.remove(accessor.getSessionId());
        log.info("Disconnected session Id : {}", accessor.getSessionId());
        log.info("Total sessions : {}", websocketSessions.size());
    }
}