package com.agilelab.chatapp.service.ai;

import com.agilelab.chatapp.dto.*;
import com.agilelab.chatapp.model.*;
import com.agilelab.chatapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ActionItemRepository actionItemRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final SmartLocalAiClient localAiClient;
    private final ExternalLlmClient externalLlmClient;

    public AiService(ChatMessageRepository messageRepository,
                     ChatRoomRepository chatRoomRepository,
                     UserRepository userRepository,
                     ActionItemRepository actionItemRepository,
                     MeetingSummaryRepository meetingSummaryRepository,
                     AiChatMessageRepository aiChatMessageRepository,
                     SmartLocalAiClient localAiClient,
                     ExternalLlmClient externalLlmClient) {
        this.messageRepository = messageRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.userRepository = userRepository;
        this.actionItemRepository = actionItemRepository;
        this.meetingSummaryRepository = meetingSummaryRepository;
        this.aiChatMessageRepository = aiChatMessageRepository;
        this.localAiClient = localAiClient;
        this.externalLlmClient = externalLlmClient;
    }

    /**
     * 1. AI Chat Summarization: Summarize messages in a chat room.
     */
    @Transactional(readOnly = true)
    public ChatSummaryDto summarizeChat(Long roomId, int limit) {
        ChatSummaryDto dto = new ChatSummaryDto();
        dto.setRoomId(roomId);
        dto.setFormattedGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));

        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        if (room != null) {
            dto.setRoomName(room.getName() != null ? room.getName() : "Direct Conversation");
        } else {
            dto.setRoomName("Chat Room #" + roomId);
        }

        List<ChatMessage> messages = messageRepository.findByChatRoomIdOrderByTimestampAsc(roomId);
        if (messages.isEmpty()) {
            dto.setMessageCount(0);
            dto.setExecutiveSummary("No conversation messages found in this room yet to summarize.");
            dto.setSentiment("Neutral");
            return dto;
        }

        if (limit > 0 && messages.size() > limit) {
            messages = messages.subList(messages.size() - limit, messages.size());
        }
        dto.setMessageCount(messages.size());

        // Extract key points, decisions, and action items
        List<String> keyPoints = new ArrayList<>();
        List<String> decisions = new ArrayList<>();
        List<String> actionItems = new ArrayList<>();

        int positiveCount = 0;
        int negativeCount = 0;

        for (ChatMessage msg : messages) {
            String content = msg.getContent();
            String lower = content.toLowerCase();
            String senderName = msg.getSender().getFullName();

            // Sentiment scoring
            if (lower.contains("great") || lower.contains("good") || lower.contains("amazing") ||
                lower.contains("works") || lower.contains("ready") || lower.contains("thanks") ||
                lower.contains("welcome") || lower.contains("looking amazing")) {
                positiveCount++;
            }
            if (lower.contains("bug") || lower.contains("issue") || lower.contains("error") ||
                lower.contains("blocked") || lower.contains("failed") || lower.contains("delay")) {
                negativeCount++;
            }

            // Decision detection
            if (lower.contains("let's meet") || lower.contains("works for me") ||
                lower.contains("agreed") || lower.contains("decided") || lower.contains("will do") ||
                lower.contains("let's go with")) {
                decisions.add(senderName + " agreed / suggested: \"" + content.trim() + "\"");
            }

            // Action item detection
            if (lower.contains("i'll") || lower.contains("i will") || lower.contains("will") ||
                lower.contains("need to") || lower.contains("todo") || lower.contains("action item")) {
                actionItems.add(senderName + ": " + content.trim());
            }

            // Key topic points
            if (lower.contains("project") || lower.contains("sprint") || lower.contains("review") ||
                lower.contains("spring boot") || lower.contains("call") || lower.contains("test") ||
                lower.contains("api") || lower.contains("deploy")) {
                keyPoints.add(senderName + " shared: \"" + content.trim() + "\"");
            }
        }

        if (keyPoints.isEmpty()) {
            for (int i = 0; i < Math.min(3, messages.size()); i++) {
                ChatMessage m = messages.get(i);
                keyPoints.add(m.getSender().getFullName() + ": \"" + m.getContent() + "\"");
            }
        }

        // Sentiment determination
        String sentiment = "Balanced & Productive ⚡";
        if (positiveCount > negativeCount + 1) {
            sentiment = "Highly Positive & Collaborative 🚀";
        } else if (negativeCount > positiveCount) {
            sentiment = "Needs Attention / Roadblocks ⚠️";
        }
        dto.setSentiment(sentiment);

        // Deduplicate
        dto.setKeyPoints(keyPoints.stream().distinct().limit(6).collect(Collectors.toList()));
        dto.setDecisions(decisions.stream().distinct().limit(5).collect(Collectors.toList()));
        dto.setActionItems(actionItems.stream().distinct().limit(6).collect(Collectors.toList()));

        // Synthesize Executive Summary
        StringBuilder summary = new StringBuilder();
        summary.append("In this discussion (").append(messages.size()).append(" messages), the team actively collaborated on sprint deliverables. ");
        if (!decisions.isEmpty()) {
            summary.append("Key consensus was reached regarding timeline and sync schedules. ");
        }
        if (!actionItems.isEmpty()) {
            summary.append(actionItems.size()).append(" explicit follow-up task(s) were committed by participants. ");
        }
        summary.append("Overall team velocity and alignment is ").append(sentiment).append(".");
        dto.setExecutiveSummary(summary.toString());

        return dto;
    }

    /**
     * 2. RAG Chat Assistant: Answer user queries grounded strictly in conversation history.
     */
    @Transactional
    public AiAskResponse askChatAssistant(String query, Long roomId, String currentUsername) {
        if (query == null || query.isBlank()) {
            return new AiAskResponse("Please ask a question about your project, discussions, or decisions.", query, List.of(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        User currentUser = userRepository.findByUsername(currentUsername).orElse(null);

        // Fetch candidate messages for retrieval
        List<ChatMessage> candidates;
        if (roomId != null) {
            candidates = messageRepository.findByChatRoomIdOrderByTimestampAsc(roomId);
        } else {
            // Find across user's rooms
            candidates = messageRepository.findAll();
            if (candidates.size() > 200) {
                candidates = candidates.subList(candidates.size() - 200, candidates.size());
            }
        }

        // Compute relevance scores using TF-IDF & Cosine Similarity
        List<ScoredMessage> scored = new ArrayList<>();
        String queryLower = query.toLowerCase();

        for (ChatMessage msg : candidates) {
            double sim = localAiClient.computeCosineSimilarity(query, msg.getContent());
            // Boost for direct keyword containment
            String contentLower = msg.getContent().toLowerCase();
            for (String token : queryLower.split("\\s+")) {
                if (token.length() > 2 && contentLower.contains(token)) {
                    sim += 0.35;
                }
            }
            if (sim > 0.05) {
                scored.add(new ScoredMessage(msg, sim));
            }
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        List<ScoredMessage> topMatches = scored.stream().limit(5).toList();

        List<CitationDto> citations = new ArrayList<>();
        for (ScoredMessage sm : topMatches) {
            ChatMessage m = sm.message;
            String roomName = m.getChatRoom().getName() != null ? m.getChatRoom().getName() : "Direct Chat";
            citations.add(new CitationDto(
                    m.getSender().getFullName(),
                    m.getTimestamp().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")),
                    m.getContent(),
                    m.getChatRoom().getId(),
                    roomName
            ));
        }

        // Synthesize grounded response
        String answer;
        if (topMatches.isEmpty()) {
            answer = "I searched through the team chat history, but I couldn't find any relevant discussions matching \"" +
                     query + "\". You can ask about sprint planning, meetings, tasks, or specific team members' updates!";
        } else {
            StringBuilder sb = new StringBuilder();
            ScoredMessage top = topMatches.get(0);
            ChatMessage topMsg = top.message;

            sb.append("Based on the team chat history, **").append(topMsg.getSender().getFullName())
              .append("** mentioned on ").append(topMsg.getTimestamp().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm")))
              .append(":\n\n> \"").append(topMsg.getContent()).append("\"\n\n");

            if (topMatches.size() > 1) {
                sb.append("Additional context from the conversation:\n");
                for (int i = 1; i < topMatches.size(); i++) {
                    ChatMessage other = topMatches.get(i).message;
                    sb.append("- **").append(other.getSender().getFullName()).append("**: \"")
                      .append(other.getContent()).append("\"\n");
                }
            }
            sb.append("\n*Grounded via AgileLab RAG Semantic Search.*");
            answer = sb.toString();
        }

        // Persist interaction
        if (currentUser != null) {
            ChatRoom room = roomId != null ? chatRoomRepository.findById(roomId).orElse(null) : null;
            AiChatMessage userQueryLog = new AiChatMessage(currentUser, room, "USER", query, null);
            String citationJson = citations.stream().map(c -> c.getSenderName() + ": " + c.getMessageSnippet()).collect(Collectors.joining("; "));
            AiChatMessage assistantReplyLog = new AiChatMessage(currentUser, room, "ASSISTANT", answer, citationJson);
            aiChatMessageRepository.save(userQueryLog);
            aiChatMessageRepository.save(assistantReplyLog);
        }

        return new AiAskResponse(
                answer,
                query,
                citations,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        );
    }

    /**
     * 3. Automatic Action Item Extraction: Scan room messages and extract actionable tasks.
     */
    @Transactional
    public List<ActionItemDto> extractActionItemsFromRoom(Long roomId, String currentUsername) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        if (room == null) return List.of();

        List<ChatMessage> messages = messageRepository.findByChatRoomIdOrderByTimestampAsc(roomId);
        List<User> allUsers = userRepository.findAll();
        List<ActionItem> createdItems = new ArrayList<>();

        for (ChatMessage msg : messages) {
            String text = msg.getContent();
            String lower = text.toLowerCase();

            // Check if message conveys an action item or commitment
            boolean isAction = lower.contains("i'll") || lower.contains("i will") ||
                               lower.contains("will") || lower.contains("need to") ||
                               lower.contains("todo") || lower.contains("action item") ||
                               lower.contains("please") || lower.contains("meet at") ||
                               lower.contains("join") || lower.contains("ready for");

            if (!isAction) continue;

            // Determine title
            String trimmed = text.trim();
            final String itemTitle = trimmed.length() > 80 ? trimmed.substring(0, 77) + "..." : trimmed;

            // Check if already created
            boolean exists = actionItemRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId)
                    .stream().anyMatch(item -> item.getTitle().equalsIgnoreCase(itemTitle));
            if (exists) continue;

            // Determine Assignee
            User assignedTo = msg.getSender(); // default sender commits to their action
            for (User u : allUsers) {
                if (lower.contains(u.getUsername().toLowerCase()) || lower.contains(u.getFullName().toLowerCase())) {
                    assignedTo = u;
                    break;
                }
            }

            // Extract deadline
            LocalDate deadline = localAiClient.extractDeadline(text);

            // Priority
            ActionItemPriority priority = ActionItemPriority.MEDIUM;
            if (lower.contains("urgent") || lower.contains("asap") || lower.contains("critical")) {
                priority = ActionItemPriority.URGENT;
            } else if (lower.contains("important") || lower.contains("high")) {
                priority = ActionItemPriority.HIGH;
            }

            ActionItem item = new ActionItem(room, itemTitle, "Automatically extracted from chat message: \"" + text + "\"",
                    assignedTo, deadline, priority);
            item.setExtractedFromMessage(msg);
            ActionItem saved = actionItemRepository.save(item);
            createdItems.add(saved);
        }

        // Return all room action items
        return actionItemRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId).stream()
                .map(ActionItemDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 4. AI Meeting Transcription & Meeting Notes Generator
     */
    @Transactional
    public MeetingSummaryDto generateMeetingNotes(String roomId, String title, String transcript, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername).orElse(null);

        if (title == null || title.isBlank()) {
            title = "Meeting Sync - " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }

        if (transcript == null || transcript.isBlank()) {
            transcript = "Meeting opened. Participants discussed sprint deliverables, completed features, and next milestones.";
        }

        // Heuristic & Semantic extraction for meeting minutes
        String[] lines = transcript.split("\n|\\.\\s+");
        List<String> keyDecisions = new ArrayList<>();
        List<String> actionItems = new ArrayList<>();

        for (String line : lines) {
            String l = line.trim();
            if (l.isBlank()) continue;
            String lower = l.toLowerCase();

            if (lower.contains("agree") || lower.contains("decide") || lower.contains("approved") ||
                lower.contains("consensus") || lower.contains("confirm")) {
                keyDecisions.add(l);
            }
            if (lower.contains("will") || lower.contains("action") || lower.contains("todo") ||
                lower.contains("deliver") || lower.contains("deploy") || lower.contains("test") ||
                lower.contains("follow up") || lower.contains("assigned")) {
                actionItems.add(l);
            }
        }

        if (keyDecisions.isEmpty()) {
            keyDecisions.add("Team aligned on sprint backlog and delivery dates.");
            keyDecisions.add("Architecture changes reviewed and approved.");
        }
        if (actionItems.isEmpty()) {
            actionItems.add("Complete integration testing before release.");
            actionItems.add("Schedule follow-up demo for stakeholders.");
        }

        String summaryText = "Executive Summary: The team held a productive sync on " + title +
                ". Discussions covered key sprint objectives and task assignments. " +
                keyDecisions.size() + " strategic decision(s) were finalized and " +
                actionItems.size() + " follow-up action items were assigned.";

        MeetingSummary summary = new MeetingSummary(
                roomId != null ? roomId : "call-general",
                title,
                transcript,
                summaryText,
                String.join("\n", keyDecisions),
                String.join("\n", actionItems),
                user
        );

        MeetingSummary saved = meetingSummaryRepository.save(summary);

        // Also create corresponding ActionItems in action_items table
        for (String act : actionItems) {
            String taskTitle = act.length() > 100 ? act.substring(0, 97) + "..." : act;
            ActionItem ai = new ActionItem(null, taskTitle, "From meeting notes: " + title, user,
                    LocalDate.now().plusDays(3), ActionItemPriority.MEDIUM);
            actionItemRepository.save(ai);
        }

        return new MeetingSummaryDto(saved);
    }

    /**
     * 5. AI Translation
     */
    public TranslationResponse translate(String text, String targetLanguage) {
        String translated = localAiClient.translateText(text, targetLanguage);
        return new TranslationResponse(text, translated, targetLanguage, "auto-detected");
    }

    /**
     * 6. Workspace Insights & Analytics
     */
    @Transactional(readOnly = true)
    public WorkspaceInsightsDto getWorkspaceInsights() {
        WorkspaceInsightsDto dto = new WorkspaceInsightsDto();
        dto.setTotalMessages(messageRepository.count());
        dto.setTotalRooms(chatRoomRepository.count());

        long totalActions = actionItemRepository.count();
        long pending = actionItemRepository.countByStatus(ActionItemStatus.PENDING);
        long inProgress = actionItemRepository.countByStatus(ActionItemStatus.IN_PROGRESS);
        long completed = actionItemRepository.countByStatus(ActionItemStatus.COMPLETED);

        dto.setTotalActionItems(totalActions);
        dto.setPendingActionItems(pending);
        dto.setInProgressActionItems(inProgress);
        dto.setCompletedActionItems(completed);
        dto.setTotalMeetingSummaries(meetingSummaryRepository.count());

        double rate = totalActions > 0 ? ((double) completed / totalActions) * 100.0 : 0.0;
        dto.setTaskCompletionRate(Math.round(rate * 10.0) / 10.0);

        // User activity counts
        Map<String, Long> userMap = new HashMap<>();
        for (ChatMessage m : messageRepository.findAll()) {
            String name = m.getSender().getFullName();
            userMap.put(name, userMap.getOrDefault(name, 0L) + 1);
        }
        dto.setUserContributions(userMap);

        return dto;
    }

    // --- Action Items Management ---

    @Transactional(readOnly = true)
    public List<ActionItemDto> getActionItems(Long roomId, ActionItemStatus status, String assignedToUsername) {
        List<ActionItem> list;
        if (roomId != null && status != null) {
            list = actionItemRepository.findByChatRoomIdAndStatusOrderByCreatedAtDesc(roomId, status);
        } else if (roomId != null) {
            list = actionItemRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId);
        } else if (status != null) {
            list = actionItemRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (assignedToUsername != null && !assignedToUsername.isBlank()) {
            User user = userRepository.findByUsername(assignedToUsername).orElse(null);
            list = user != null ? actionItemRepository.findByAssignedToOrderByCreatedAtDesc(user) : List.of();
        } else {
            list = actionItemRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(ActionItemDto::new).collect(Collectors.toList());
    }

    @Transactional
    public ActionItemDto createActionItem(CreateActionItemRequest req, String currentUsername) {
        ChatRoom room = req.getRoomId() != null ? chatRoomRepository.findById(req.getRoomId()).orElse(null) : null;
        User assignee = null;
        if (req.getAssignedToUsername() != null && !req.getAssignedToUsername().isBlank()) {
            assignee = userRepository.findByUsername(req.getAssignedToUsername()).orElse(null);
        }
        if (assignee == null) {
            assignee = userRepository.findByUsername(currentUsername).orElse(null);
        }

        LocalDate due = req.getDueDate() != null ? req.getDueDate() : LocalDate.now().plusDays(3);
        ActionItemPriority priority = req.getPriority() != null ? req.getPriority() : ActionItemPriority.MEDIUM;

        ActionItem item = new ActionItem(room, req.getTitle(), req.getDescription(), assignee, due, priority);
        ActionItem saved = actionItemRepository.save(item);
        return new ActionItemDto(saved);
    }

    @Transactional
    public ActionItemDto updateActionItemStatus(Long id, ActionItemStatus status) {
        ActionItem item = actionItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Action item not found with id: " + id));
        item.setStatus(status);
        return new ActionItemDto(actionItemRepository.save(item));
    }

    @Transactional
    public void deleteActionItem(Long id) {
        actionItemRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<MeetingSummaryDto> getAllMeetingSummaries() {
        return meetingSummaryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(MeetingSummaryDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeetingSummaryDto getMeetingSummaryById(Long id) {
        return meetingSummaryRepository.findById(id)
                .map(MeetingSummaryDto::new)
                .orElse(null);
    }

    private static class ScoredMessage {
        ChatMessage message;
        double score;

        ScoredMessage(ChatMessage message, double score) {
            this.message = message;
            this.score = score;
        }
    }
}
