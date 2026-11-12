package com.agilelab.chatapp;

import com.agilelab.chatapp.dto.MessageDto;
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
    "spring.datasource.url=jdbc:h2:mem:wstestdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class WebSocketMessagingIntegrationTests {

    @LocalServerPort
    private int port;

    @Test
    void testRealTimeWebSocketMessageSendingAndReceiving() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String wsUrl = "ws://localhost:" + port + "/ws";
        CompletableFuture<Map<String, Object>> messageFuture = new CompletableFuture<>();

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {}).get(8, TimeUnit.SECONDS);
        assertNotNull(session, "STOMP Session should be connected");

        // Subscribe to room 1 topic
        session.subscribe("/topic/room.1", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageFuture.complete((Map<String, Object>) payload);
            }
        });

        // Small pause to ensure broker subscription is registered
        Thread.sleep(800);

        // Send a test message to /app/chat.sendMessage
        session.send("/app/chat.sendMessage", Map.of(
                "roomId", 1,
                "content", "Real-time WebSocket Test Message!",
                "senderUsername", "alice",
                "type", "CHAT"
        ));

        // Wait for broadcast on /topic/room.1
        Map<String, Object> received = messageFuture.get(8, TimeUnit.SECONDS);
        assertNotNull(received, "Should receive broadcasted message from /topic/room.1");
        assertEquals("Real-time WebSocket Test Message!", received.get("content"));
        assertEquals("alice", received.get("senderUsername"));

        session.disconnect();
    }
}
