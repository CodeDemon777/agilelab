package com.agilelab.chatapp;

import com.agilelab.chatapp.dto.ChatRoomDto;
import com.agilelab.chatapp.dto.MessageDto;
import com.agilelab.chatapp.dto.RegisterRequest;
import com.agilelab.chatapp.model.ChatRoom;
import com.agilelab.chatapp.model.MessageType;
import com.agilelab.chatapp.model.User;
import com.agilelab.chatapp.service.ChatService;
import com.agilelab.chatapp.service.NotificationService;
import com.agilelab.chatapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:apptestdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AgileLabApplicationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private NotificationService notificationService;

    @Test
    void contextLoads() {
        assertNotNull(userService);
        assertNotNull(chatService);
        assertNotNull(notificationService);
    }

    @Test
    void testPreloadedUsers() {
        User john = userService.findByUsername("johndoe");
        assertNotNull(john);
        assertEquals("John Doe", john.getFullName());
        assertEquals("Living the dream", john.getStatusQuote());

        User alice = userService.findByUsername("alice");
        assertNotNull(alice);
        assertEquals("Alice Smith", alice.getFullName());
    }

    @Test
    void testRegisterNewUser() {
        String testUser = "testuser_" + System.currentTimeMillis();
        RegisterRequest req = new RegisterRequest();
        req.setUsername(testUser);
        req.setEmail(testUser + "@example.com");
        req.setPassword("secret123");
        req.setFullName("Test User");

        User registered = userService.register(req);
        assertNotNull(registered);
        assertEquals(testUser, registered.getUsername());

        // Verify finding by username
        User found = userService.findByUsername(testUser);
        assertNotNull(found);
        assertEquals("Test User", found.getFullName());
    }

    @Test
    void testDirectChatAndMessaging() {
        // Find or create direct chat between Alice and Bob
        ChatRoom room = chatService.getOrCreateDirectRoom("alice", "bob");
        assertNotNull(room);

        // Send a message
        MessageDto msg = chatService.saveMessage(room.getId(), "alice", "Test message from Alice to Bob", MessageType.CHAT, null);
        assertNotNull(msg);
        assertEquals("Test message from Alice to Bob", msg.getContent());
        assertEquals("alice", msg.getSenderUsername());

        // Retrieve messages
        List<MessageDto> messages = chatService.getRoomMessages(room.getId(), "bob");
        assertFalse(messages.isEmpty());
        assertTrue(messages.stream().anyMatch(m -> m.getContent().equals("Test message from Alice to Bob")));
    }

    @Test
    void testChatRoomListForUser() {
        List<ChatRoomDto> rooms = chatService.getUserChatRooms("johndoe");
        assertNotNull(rooms);
        assertFalse(rooms.isEmpty());
    }
}

