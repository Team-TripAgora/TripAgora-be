package com.tripagora.common.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {
    public void sendMulticastMessage(List<String> tokens, String title, String body, Long tripId) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        AndroidConfig androidConfig = AndroidConfig.builder()
                .setNotification(AndroidNotification.builder()
                        .setChannelId("default")
                        .build())
                .build();

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(notification)
                .setAndroidConfig(androidConfig)
                .addAllTokens(tokens)
                .putData("type", "SOS")
                .putData("sessionId", tripId.toString())
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.info("FCM 전송 결과 - 대상: {}건, 성공: {}건, 실패: {}건",
                    tokens.size(), response.getSuccessCount(), response.getFailureCount());
        } catch (Exception e) {
            log.error("FCM 전송 실패", e);
        }
    }
}