package com.agilelab.chatapp.controller;

import com.agilelab.chatapp.dto.*;
import com.agilelab.chatapp.model.ActionItemStatus;
import com.agilelab.chatapp.service.ai.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiRestController {

    private final AiService aiService;

    public AiRestController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Summarize chat room conversation.
     */
    @GetMapping("/summarize/{roomId}")
    public ResponseEntity<ChatSummaryDto> summarizeGet(@PathVariable Long roomId,
                                                       @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(aiService.summarizeChat(roomId, limit));
    }

    @PostMapping("/summarize/{roomId}")
    public ResponseEntity<ChatSummaryDto> summarizePost(@PathVariable Long roomId,
                                                        @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ResponseEntity.ok(aiService.summarizeChat(roomId, limit));
    }

    /**
     * Ask AI Chat Assistant (RAG query across chat history).
     */
    @PostMapping("/ask")
    public ResponseEntity<AiAskResponse> ask(@RequestBody AiAskRequest request, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        return ResponseEntity.ok(aiService.askChatAssistant(request.getQuery(), request.getRoomId(), username));
    }

    /**
     * Automatic Action Item Extraction from a room.
     */
    @PostMapping("/action-items/extract/{roomId}")
    public ResponseEntity<List<ActionItemDto>> extractActionItems(@PathVariable Long roomId, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        return ResponseEntity.ok(aiService.extractActionItemsFromRoom(roomId, username));
    }

    /**
     * Retrieve action items with optional filters.
     */
    @GetMapping("/action-items")
    public ResponseEntity<List<ActionItemDto>> getActionItems(
            @RequestParam(value = "roomId", required = false) Long roomId,
            @RequestParam(value = "status", required = false) ActionItemStatus status,
            @RequestParam(value = "assignedTo", required = false) String assignedTo) {
        return ResponseEntity.ok(aiService.getActionItems(roomId, status, assignedTo));
    }

    /**
     * Manually create action item.
     */
    @PostMapping("/action-items")
    public ResponseEntity<ActionItemDto> createActionItem(@RequestBody CreateActionItemRequest request,
                                                          Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        return ResponseEntity.ok(aiService.createActionItem(request, username));
    }

    /**
     * Update action item status.
     */
    @PostMapping("/action-items/{id}/status")
    public ResponseEntity<ActionItemDto> updateStatus(@PathVariable Long id,
                                                      @RequestBody UpdateActionItemStatusRequest req) {
        return ResponseEntity.ok(aiService.updateActionItemStatus(id, req.getStatus()));
    }

    /**
     * Delete action item.
     */
    @DeleteMapping("/action-items/{id}")
    public ResponseEntity<Map<String, String>> deleteActionItem(@PathVariable Long id) {
        aiService.deleteActionItem(id);
        return ResponseEntity.ok(Map.of("message", "Action item deleted successfully"));
    }

    /**
     * Meeting Notes generation & transcription ingestion.
     */
    @PostMapping("/meeting-notes")
    public ResponseEntity<MeetingSummaryDto> generateMeetingNotes(@RequestBody GenerateMeetingNotesRequest req,
                                                                  Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        return ResponseEntity.ok(aiService.generateMeetingNotes(req.getRoomId(), req.getTitle(), req.getTranscript(), username));
    }

    @GetMapping("/meeting-notes")
    public ResponseEntity<List<MeetingSummaryDto>> getAllMeetingNotes() {
        return ResponseEntity.ok(aiService.getAllMeetingSummaries());
    }

    @GetMapping("/meeting-notes/{id}")
    public ResponseEntity<MeetingSummaryDto> getMeetingNoteById(@PathVariable Long id) {
        MeetingSummaryDto dto = aiService.getMeetingSummaryById(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * Multilingual AI Translation.
     */
    @PostMapping("/translate")
    public ResponseEntity<TranslationResponse> translate(@RequestBody TranslationRequest req) {
        return ResponseEntity.ok(aiService.translate(req.getText(), req.getTargetLanguage()));
    }

    /**
     * Workspace AI Insights & Analytics.
     */
    @GetMapping("/insights")
    public ResponseEntity<WorkspaceInsightsDto> getInsights() {
        return ResponseEntity.ok(aiService.getWorkspaceInsights());
    }
}

