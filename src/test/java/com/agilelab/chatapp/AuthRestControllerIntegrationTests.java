package com.agilelab.chatapp;

import com.agilelab.chatapp.dto.LoginRequest;
import com.agilelab.chatapp.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:authtestdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthRestControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegisterApi_Success() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser1");
        req.setEmail("newuser1@example.com");
        req.setPassword("securepass123");
        req.setFullName("New User One");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.username").value("newuser1"))
                .andExpect(jsonPath("$.user.fullName").value("New User One"));
    }

    @Test
    void testRegisterApi_DuplicateUsername() throws Exception {
        // "johndoe" is already preloaded by DataInitializer
        RegisterRequest req = new RegisterRequest();
        req.setUsername("johndoe");
        req.setEmail("unique_email@example.com");
        req.setPassword("securepass123");
        req.setFullName("Another John");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already taken")));
    }

    @Test
    void testRegisterApi_DuplicateEmail() throws Exception {
        // "alice@example.com" is already preloaded
        RegisterRequest req = new RegisterRequest();
        req.setUsername("brandnewuser2");
        req.setEmail("alice@example.com");
        req.setPassword("securepass123");
        req.setFullName("Duplicate Email User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already in use")));
    }

    @Test
    void testRegisterApi_ValidationFailure_InvalidEmail() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("invaliduser");
        req.setEmail("not-an-email");
        req.setPassword("securepass123");
        req.setFullName("Invalid Email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRegisterApi_ValidationFailure_ShortPassword() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("shortpassuser");
        req.setEmail("shortpass@example.com");
        req.setPassword("123"); // too short
        req.setFullName("Short Pass");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testLoginApi_Success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("johndoe");
        req.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.user.username").value("johndoe"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotNull(session, "Session should be established on login");
    }

    @Test
    void testLoginApi_WrongPassword() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("johndoe");
        req.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void testLoginApi_NonExistentUser() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("ghostuser_xyz");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void testGetCurrentUser_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @WithMockUser(username = "alice")
    void testGetCurrentUser_Authenticated() throws Exception {
        mockMvc.perform(get("/api/auth/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.fullName").value("Alice Smith"));
    }

    @Test
    void testLogoutApi() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void testFormSignup_SuccessRedirect() throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("username", "formuser1")
                        .param("email", "formuser1@example.com")
                        .param("password", "formpassword123")
                        .param("fullName", "Form User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));
    }

    @Test
    void testFormSignup_ValidationFailure() throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("username", "") // blank
                        .param("email", "invalid")
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void testFormSignup_DuplicateUser() throws Exception {
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("username", "bob") // already exists
                        .param("email", "bob_new@example.com")
                        .param("password", "bobpassword123")
                        .param("fullName", "Bob Duplicate"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attributeExists("errorMessage"));
    }
}
