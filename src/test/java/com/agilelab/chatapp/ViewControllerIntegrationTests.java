package com.agilelab.chatapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:viewtestdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ViewControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testLandingPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"));
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("AgileLab-Backend"));

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void testLoginPageWithError() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void testLoginPageWithLogout() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("successMessage"));
    }

    @Test
    void testSignupPage() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

    @Test
    void testChatsPage_UnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/chats"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testChatsPage_Authenticated() throws Exception {
        mockMvc.perform(get("/chats"))
                .andExpect(status().isOk())
                .andExpect(view().name("chats"))
                .andExpect(model().attributeExists("rooms"))
                .andExpect(model().attributeExists("currentUser"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testCallPage_Authenticated() throws Exception {
        mockMvc.perform(get("/call/test-room-1").param("target", "alice").param("type", "video"))
                .andExpect(status().isOk())
                .andExpect(view().name("call"))
                .andExpect(model().attribute("roomId", "test-room-1"))
                .andExpect(model().attribute("targetUsername", "alice"))
                .andExpect(model().attribute("callType", "VIDEO"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testNotificationsPage_Authenticated() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(model().attributeExists("notifications"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testSelfProfilePage_Authenticated() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("isSelf", true))
                .andExpect(model().attributeExists("profileUser"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testOtherUserProfilePage_Authenticated() throws Exception {
        mockMvc.perform(get("/profile/alice"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("isSelf", false))
                .andExpect(model().attributeExists("profileUser"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testOtherUserProfilePage_NonExistentRedirects() throws Exception {
        mockMvc.perform(get("/profile/non_existent_ghost_user"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/chats"));
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testAiWorkspacePage_Authenticated() throws Exception {
        mockMvc.perform(get("/ai-workspace"))
                .andExpect(status().isOk())
                .andExpect(view().name("ai-workspace"))
                .andExpect(model().attributeExists("insights"))
                .andExpect(model().attributeExists("rooms"));
    }
}

