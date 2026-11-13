package com.agilelab.chatapp;

import com.agilelab.chatapp.dto.*;
import com.agilelab.chatapp.model.ActionItemPriority;
import com.agilelab.chatapp.model.ActionItemStatus;
import com.agilelab.chatapp.model.ChatRoom;
import com.agilelab.chatapp.repository.ChatRoomRepository;
import com.agilelab.chatapp.service.ai.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:aitestdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AiCollaborationIntegrationTests {

    @Autowired
    private AiService aiService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testChatSummarization() {
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        assertFalse(rooms.isEmpty(), "Should have seeded chat rooms");

        ChatRoom target = rooms.stream().filter(ChatRoom::isGroup).findFirst().orElse(rooms.get(0));
        ChatSummaryDto summary = aiService.summarizeChat(target.getId(), 50);

        assertNotNull(summary);
        assertEquals(target.getId(), summary.getRoomId());
        assertTrue(summary.getMessageCount() > 0, "Should have analyzed messages");
        assertNotNull(summary.getExecutiveSummary());
        assertFalse(summary.getExecutiveSummary().isBlank());
        assertNotNull(summary.getSentiment());
        assertNotNull(summary.getDecisions());
        assertNotNull(summary.getActionItems());
    }

    @Test
    void testRagAssistantGrounding() {
        AiAskResponse response = aiService.askChatAssistant("Who is working on Docker container?", null, "johndoe");

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertFalse(response.getCitations().isEmpty(), "RAG pipeline should find grounded citation from Bob's message");
        assertTrue(response.getAnswer().toLowerCase().contains("bob") || response.getCitations().get(0).getSenderName().contains("Bob"));
    }

    @Test
    void testActionItemExtraction() {
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        ChatRoom group = rooms.stream().filter(ChatRoom::isGroup).findFirst().orElse(rooms.get(0));

        List<ActionItemDto> items = aiService.extractActionItemsFromRoom(group.getId(), "johndoe");
        assertNotNull(items);
        assertFalse(items.isEmpty(), "Should have extracted action items from group chat");

        ActionItemDto first = items.get(0);
        assertNotNull(first.getTitle());
        assertNotNull(first.getStatus());
        assertNotNull(first.getPriority());
    }

    @Test
    void testActionItemCrudLifecycle() {
        CreateActionItemRequest req = new CreateActionItemRequest();
        req.setTitle("Implement Spring AI RAG Evaluation Suite");
        req.setDescription("Unit and integration tests for retrieval accuracy.");
        req.setAssignedToUsername("alice");
        req.setDueDate(LocalDate.now().plusDays(4));
        req.setPriority(ActionItemPriority.HIGH);

        ActionItemDto created = aiService.createActionItem(req, "johndoe");
        assertNotNull(created.getId());
        assertEquals("Implement Spring AI RAG Evaluation Suite", created.getTitle());
        assertEquals(ActionItemStatus.PENDING, created.getStatus());
        assertEquals(ActionItemPriority.HIGH, created.getPriority());

        // Toggle status to COMPLETED
        ActionItemDto updated = aiService.updateActionItemStatus(created.getId(), ActionItemStatus.COMPLETED);
        assertEquals(ActionItemStatus.COMPLETED, updated.getStatus());

        // Delete item
        aiService.deleteActionItem(created.getId());
        List<ActionItemDto> remaining = aiService.getActionItems(null, null, null);
        assertTrue(remaining.stream().noneMatch(i -> i.getId().equals(created.getId())));
    }

    @Test
    void testGenerateMeetingNotes() {
        String transcript = "John: Let's finalize the WebSocket STOMP and Spring AI integration.\n" +
                "Alice: I agree, the RAG assistant is performing fast with local fallback.\n" +
                "Bob: I will deploy the build to staging environment by tomorrow.\n" +
                "Emma: Approved, I will prepare the presentation slides.";

        MeetingSummaryDto summary = aiService.generateMeetingNotes("call-sync-test", "Sprint 4 AI Review", transcript, "johndoe");

        assertNotNull(summary);
        assertNotNull(summary.getId());
        assertEquals("Sprint 4 AI Review", summary.getTitle());
        assertNotNull(summary.getSummaryText());
        assertFalse(summary.getKeyDecisions().isEmpty(), "Decisions should be identified");
        assertFalse(summary.getActionItems().isEmpty(), "Action items should be identified");
    }

    @Test
    void testMultilingualTranslation() {
        TranslationResponse es = aiService.translate("Hello! How are you?", "spanish");
        assertNotNull(es);
        assertEquals("spanish", es.getTargetLanguage());
        assertTrue(es.getTranslatedText().toLowerCase().contains("hola") || es.getTranslatedText().contains("cómo estás"));

        TranslationResponse fr = aiService.translate("Welcome to the team", "french");
        assertNotNull(fr);
        assertTrue(fr.getTranslatedText().toLowerCase().contains("bienvenue") || fr.getTranslatedText().contains("[FR]"));
    }

    @Test
    void testWorkspaceInsights() {
        WorkspaceInsightsDto insights = aiService.getWorkspaceInsights();
        assertNotNull(insights);
        assertTrue(insights.getTotalMessages() > 0);
        assertTrue(insights.getTotalRooms() > 0);
        assertTrue(insights.getTotalActionItems() > 0);
        assertNotNull(insights.getUserContributions());
        assertFalse(insights.getUserContributions().isEmpty());
    }

    @Test
    @WithMockUser(username = "johndoe")
    void testAiRestControllerEndpoints() throws Exception {
        List<ChatRoom> rooms = chatRoomRepository.findAll();
        Long roomId = rooms.get(0).getId();

        // 1. GET /api/ai/summarize/{roomId}
        mockMvc.perform(get("/api/ai/summarize/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId))
                .andExpect(jsonPath("$.executiveSummary").isNotEmpty());

        // 2. POST /api/ai/ask
        mockMvc.perform(post("/api/ai/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"What is the meeting time?\",\"roomId\":" + roomId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty());

        // 3. GET /api/ai/action-items
        mockMvc.perform(get("/api/ai/action-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 4. POST /api/ai/translate
        mockMvc.perform(post("/api/ai/translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Hello\",\"targetLanguage\":\"spanish\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translatedText").isNotEmpty());

        // 5. GET /api/ai/insights
        mockMvc.perform(get("/api/ai/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMessages").isNumber());

        // 6. GET /ai-workspace page view
        mockMvc.perform(get("/ai-workspace"))
                .andExpect(status().isOk())
                .andExpect(view().name("ai-workspace"));
    }
}

