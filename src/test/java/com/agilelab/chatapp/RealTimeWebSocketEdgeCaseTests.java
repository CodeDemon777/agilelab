package com.agilelab.chatapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:wsedgetestdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RealTimeWebSocketEdgeCaseTests {

    @LocalServerPort
    private int port;

    private WebSocketStompClient createStompClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        return client;
    }

    @Test
    void testRealTimeTypingIndicatorBroadcasting() throws Exception {
        WebSocketStompClient stompClient = createStompClient();
        String wsUrl = "ws://localhost:" + port + "/ws";
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {}).get(8, TimeUnit.SECONDS);

        CompletableFuture<Map<String, Object>> typingFuture = new CompletableFuture<>();

        session.subscribe("/topic/room.1.typing", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                typingFuture.complete((Map<String, Object>) payload);
            }
        });

        Thread.sleep(600);

        session.send("/app/chat.typing", Map.of(
                "roomId", 1,
                "username", "alice",
                "isTyping", true
        ));

        Map<String, Object> received = typingFuture.get(8, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(1, received.get("roomId"));
        assertEquals("alice", received.get("username"));
        assertEquals(true, received.get("isTyping"));

        session.disconnect();
    }

    @Test
    void testRealTimeWebRtcCallSignaling() throws Exception {
        WebSocketStompClient stompClient = createStompClient();
        String wsUrl = "ws://localhost:" + port + "/ws";
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {}).get(8, TimeUnit.SECONDS);

        CompletableFuture<Map<String, Object>> callSignalFuture = new CompletableFuture<>();

        session.subscribe("/topic/call.meet-404", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                callSignalFuture.complete((Map<String, Object>) payload);
            }
        });

        Thread.sleep(600);

        // Send a call signal without a specific toUser so it broadcasts to /topic/call.{roomId}
        session.send("/app/call.signal", Map.of(
                "roomId", "meet-404",
                "fromUser", "alice",
                "type", "INVITE",
                "callType", "VIDEO"
        ));

        Map<String, Object> received = callSignalFuture.get(8, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals("meet-404", received.get("roomId"));
        assertEquals("alice", received.get("fromUser"));
        assertEquals("Alice Smith", received.get("fromFullName")); // enriched from DB!
        assertEquals("INVITE", received.get("type"));

        session.disconnect();
    }

    @Test
    void testRealTimeGlobalMessagesBroadcast() throws Exception {
        WebSocketStompClient stompClient = createStompClient();
        String wsUrl = "ws://localhost:" + port + "/ws";
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {}).get(8, TimeUnit.SECONDS);

        CompletableFuture<Map<String, Object>> globalMsgFuture = new CompletableFuture<>();

        session.subscribe("/topic/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                globalMsgFuture.complete((Map<String, Object>) payload);
            }
        });

        Thread.sleep(600);

        // Alice sends message to room 1
        session.send("/app/chat.sendMessage", Map.of(
                "roomId", 1,
                "content", "Global Topic Preview Test",
                "senderUsername", "alice",
                "type", "CHAT"
        ));

        Map<String, Object> received = globalMsgFuture.get(8, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals("Global Topic Preview Test", received.get("content"));
        assertEquals("alice", received.get("senderUsername"));

        session.disconnect();
    }

    @Test
    void testUnauthorizedRoomMessageRejectedByWebSocket() throws Exception {
        WebSocketStompClient stompClient = createStompClient();
        String wsUrl = "ws://localhost:" + port + "/ws";
        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {}).get(8, TimeUnit.SECONDS);

        CompletableFuture<Map<String, Object>> roomMsgFuture = new CompletableFuture<>();

        session.subscribe("/topic/room.1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                roomMsgFuture.complete((Map<String, Object>) payload);
            }
        });

        Thread.sleep(600);

        // Emma is NOT a participant in room 1 (which is only between johndoe and alice)
        session.send("/app/chat.sendMessage", Map.of(
                "roomId", 1,
                "content", "Intruder message from Emma",
                "senderUsername", "emma",
                "type", "CHAT"
        ));

        // The unauthorized message should NOT be delivered to /topic/room.1
        assertThrows(Exception.class, () -> roomMsgFuture.get(2, TimeUnit.SECONDS),
                "Non-member should NOT be able to broadcast messages to room 1");

        session.disconnect();
    }
}
