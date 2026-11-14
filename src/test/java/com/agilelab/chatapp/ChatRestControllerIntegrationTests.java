package com.agilelab.chatapp;

import com.agilelab.chatapp.dto.CreateGroupRequest;
import com.agilelab.chatapp.dto.ProfileUpdateRequest;
import com.agilelab.chatapp.model.ChatRoom;
import com.agilelab.chatapp.repository.ChatRoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:chattestdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ChatRestControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Test
    void testGetRooms_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/chats"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testGetRooms_Authenticated() throws Exception {
        mockMvc.perform(get("/api/chats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testGetRoomDetails_AsMember() throws Exception {
        // Room 1 was seeded between johndoe and alice
        mockMvc.perform(get("/api/chats/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.group").value(false));
    }

    @Test
    @WithMockUser(username = "emma")
    void testGetRoomDetails_AsNonMember_Forbidden() throws Exception {
        // Room 1 is direct chat between johndoe and alice, Emma is NOT a member
        mockMvc.perform(get("/api/chats/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testGetRoomDetails_NonExistentRoom() throws Exception {
        mockMvc.perform(get("/api/chats/99999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testGetRoomMessages_AsMember() throws Exception {
        mockMvc.perform(get("/api/chats/1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].content").exists());
    }

    @Test
    @WithMockUser(username = "emma")
    void testGetRoomMessages_AsNonMember_Forbidden() throws Exception {
        mockMvc.perform(get("/api/chats/1/messages"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testSendMessageRest_AsMember() throws Exception {
        Map<String, String> payload = Map.of("content", "Hello from REST test!");

        mockMvc.perform(post("/api/chats/1/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Hello from REST test!"))
                .andExpect(jsonPath("$.senderUsername").value("johndoe"));
    }

    @Test
    @WithMockUser(username = "emma")
    void testSendMessageRest_AsNonMember_Forbidden() throws Exception {
        Map<String, String> payload = Map.of("content", "Unauthorized intrusion attempt");

        mockMvc.perform(post("/api/chats/1/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testSendMessageRest_EmptyContent() throws Exception {
        Map<String, String> payload = Map.of("content", "   ");

        mockMvc.perform(post("/api/chats/1/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot be empty")));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testCreateDirectChat_Success() throws Exception {
        Map<String, String> payload = Map.of("username", "emma");

        mockMvc.perform(post("/api/chats/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group").value(false))
                .andExpect(jsonPath("$.partnerUsername").value("emma"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testCreateDirectChat_SelfError() throws Exception {
        Map<String, String> payload = Map.of("username", "johndoe");

        mockMvc.perform(post("/api/chats/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot create a direct chat with yourself")));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testCreateDirectChat_MissingUsername() throws Exception {
        Map<String, String> payload = Map.of("username", "  ");

        mockMvc.perform(post("/api/chats/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Target username is required")));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testCreateDirectChat_NonExistentUser() throws Exception {
        Map<String, String> payload = Map.of("username", "ghost_target_999");

        mockMvc.perform(post("/api/chats/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("User not found")));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testCreateGroupChat_Success() throws Exception {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Sprint 42 Warriors");
        req.setMemberUsernames(List.of("alice", "bob"));

        mockMvc.perform(post("/api/chats/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group").value(true))
                .andExpect(jsonPath("$.displayName").value("Sprint 42 Warriors"))
                .andExpect(jsonPath("$.members", hasSize(3))); // johndoe (creator) + alice + bob
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testCreateGroupChat_BlankName() throws Exception {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("   ");
        req.setMemberUsernames(List.of("alice"));

        mockMvc.perform(post("/api/chats/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testAddMembersToGroup_Success() throws Exception {
        // First create a group
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Frontend Guild");
        req.setMemberUsernames(List.of("alice"));

        String responseJson = mockMvc.perform(post("/api/chats/group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long groupId = objectMapper.readTree(responseJson).get("id").asLong();

        // Now add "emma" and "bob"
        Map<String, List<String>> addPayload = Map.of("usernames", List.of("emma", "bob"));

        mockMvc.perform(post("/api/chats/" + groupId + "/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testAddMembersToDirectChat_Error() throws Exception {
        // Room 1 is a direct chat, cannot add members to it
        Map<String, List<String>> addPayload = Map.of("usernames", List.of("emma"));

        mockMvc.perform(post("/api/chats/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot add members to a 1-on-1 chat")));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testAddMembers_EmptyList() throws Exception {
        Map<String, List<String>> addPayload = Map.of("usernames", List.of());

        mockMvc.perform(post("/api/chats/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Usernames list cannot be empty")));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testMarkRoomAsRead() throws Exception {
        mockMvc.perform(post("/api/chats/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testSearchUsers() throws Exception {
        mockMvc.perform(get("/api/users/search").param("q", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("alice"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testSearchUsers_EmptyQuery() throws Exception {
        mockMvc.perform(get("/api/users/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].username", not(hasItem("johndoe"))));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].username", not(hasItem("johndoe"))));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testUpdateProfile_Success() throws Exception {
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setFullName("John Doe Supercharged");
        req.setStatusQuote("Architecting Cloud Native Apps");
        req.setAvatarUrl("https://example.com/avatar.png");

        mockMvc.perform(post("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.fullName").value("John Doe Supercharged"))
                .andExpect(jsonPath("$.user.statusQuote").value("Architecting Cloud Native Apps"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testUpdateProfile_DuplicateEmail() throws Exception {
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setEmail("alice@example.com"); // already used by alice

        mockMvc.perform(post("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already used by another account")));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testUpdateProfile_PasswordChange_WrongCurrentPassword() throws Exception {
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setCurrentPassword("wrongCurrentPass");
        req.setNewPassword("brandNewPassword123");

        mockMvc.perform(post("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Current password does not match")));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testUpdateProfile_PasswordChange_ShortNewPassword() throws Exception {
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setCurrentPassword("password123");
        req.setNewPassword("123"); // too short

        mockMvc.perform(post("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("at least 6 characters")));
    }
}
