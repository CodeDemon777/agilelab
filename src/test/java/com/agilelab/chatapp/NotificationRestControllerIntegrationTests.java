package com.agilelab.chatapp;

import com.agilelab.chatapp.model.Notification;
import com.agilelab.chatapp.model.NotificationType;
import com.agilelab.chatapp.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:notiftestdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationRestControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationService notificationService;

    @Test
    @WithMockUser(username = "johndoe")
    void testGetNotificationsAndMarkReadFlow() throws Exception {
        // Seed 2 notifications for johndoe
        Notification n1 = notificationService.createNotification(
                "johndoe", "alice", "Review PR #12", "Please review PR 12", "/chats", NotificationType.SYSTEM
        );
        Notification n2 = notificationService.createNotification(
                "johndoe", "bob", "Sprint Planning", "Sprint planning at 3 PM", "/chats", NotificationType.SYSTEM
        );

        // 1. Get notifications
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].title", hasItems("Review PR #12", "Sprint Planning")));

        // 2. Check unread count
        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", greaterThanOrEqualTo(2)));

        // 3. Mark single notification as read
        mockMvc.perform(post("/api/notifications/" + n1.getId() + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Mark all as read
        mockMvc.perform(post("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. Unread count should now be 0
        long unreadCount = notificationService.getUnreadCount("johndoe");
        assertEquals(0, unreadCount);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @WithMockUser(username = "alice")
    void testNotificationUserIsolation() throws Exception {
        // Create notification specifically for bob
        Notification forBob = notificationService.createNotification(
                "bob", "emma", "Private Sync", "Sync on WebRTC", "/call", NotificationType.CALL
        );

        // Alice attempts to mark Bob's notification as read
        mockMvc.perform(post("/api/notifications/" + forBob.getId() + "/read"))
                .andExpect(status().isOk());

        // For Bob it must remain unread!
        long bobUnread = notificationService.getUnreadCount("bob");
        assertEquals(1, bobUnread, "Bob's notification must not be marked read by Alice");
    }
}
