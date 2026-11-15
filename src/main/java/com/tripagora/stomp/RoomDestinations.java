package com.tripagora.stomp;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 채팅방 STOMP 주소 규칙을 한 곳에 둔다. 인터셉터는 이 규칙으로 검사하고, 컨트롤러는 보낼 주소를 여기서 만든다
// (규칙이 발행 쪽과 인가 쪽에 흩어져 있으면, 채널을 바꿀 때 한쪽만 고쳐 구독이 전부 거부될 수 있다)
//   구독: /topic/room/{roomId}/{채널}    발행: /publish/room/{roomId}/{채널}    채널: chat, location
public final class RoomDestinations {
    public static final String CHAT = "chat";
    public static final String LOCATION = "location";

    private static final String CHANNELS = "(" + CHAT + "|" + LOCATION + ")";
    private static final Pattern SUBSCRIPTION = Pattern.compile("/topic/room/(\\d{1,18})/" + CHANNELS);
    private static final Pattern PUBLISH = Pattern.compile("/publish/room/(\\d{1,18})/" + CHANNELS);

    private RoomDestinations() {
    }

    // 구독 주소에서 채팅방 id를 꺼낸다. 형식이 틀리면 비어 있다
    public static Optional<Long> roomIdOfSubscription(String destination) {
        return roomIdOf(SUBSCRIPTION, destination);
    }

    // 발행 주소에서 채팅방 id를 꺼낸다. 형식이 틀리면 비어 있다
    public static Optional<Long> roomIdOfPublish(String destination) {
        return roomIdOf(PUBLISH, destination);
    }

    // 채팅방 구독자에게 보낼 주소
    public static String topic(long roomId, String channel) {
        return "/topic/room/" + roomId + "/" + channel;
    }

    private static Optional<Long> roomIdOf(Pattern pattern, String destination) {
        if (destination == null) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(destination);
        return matcher.matches() ? Optional.of(Long.parseLong(matcher.group(1))) : Optional.empty();
    }
}
