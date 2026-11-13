package com.agilelab.chatapp.controller;

import com.agilelab.chatapp.dto.*;
import com.agilelab.chatapp.model.ChatRoom;
import com.agilelab.chatapp.model.MessageType;
import com.agilelab.chatapp.model.User;
import com.agilelab.chatapp.service.ChatService;
import com.agilelab.chatapp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatRestController {

    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatRestController(ChatService chatService,
                              UserService userService,
                              SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping({"/chats", "/chat/rooms"})
    public ResponseEntity<List<ChatRoomDto>> getRooms(Principal principal) {
        return ResponseEntity.ok(chatService.getUserChatRooms(principal.getName()));
    }

    @GetMapping({"/chats/{roomId}", "/chat/rooms/{roomId}"})
    public ResponseEntity<ChatRoomDto> getRoom(@PathVariable Long roomId, Principal principal) {
        return ResponseEntity.ok(chatService.getChatRoomDetails(roomId, principal.getName()));
    }

    @GetMapping({"/chats/{roomId}/messages", "/chat/rooms/{roomId}/messages"})
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable Long roomId, Principal principal) {
        return ResponseEntity.ok(chatService.getRoomMessages(roomId, principal.getName()));
    }

    @PostMapping({"/chats/direct", "/chat/direct"})
    public ResponseEntity<?> createDirectChat(@RequestBody Map<String, String> payload, Principal principal) {
        String targetUsername = payload.get("username");
        if (targetUsername == null || targetUsername.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Target username is required"));
        }
        try {
            ChatRoom room = chatService.getOrCreateDirectRoom(principal.getName(), targetUsername.trim());
            ChatRoomDto dto = chatService.getChatRoomDetails(room.getId(), principal.getName());
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/chats/group")
    public ResponseEntity<?> createGroup(@RequestBody @Valid CreateGroupRequest request, Principal principal) {
        try {
            ChatRoom room = chatService.createGroupRoom(principal.getName(), request);
            ChatRoomDto dto = chatService.getChatRoomDetails(room.getId(), principal.getName());
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/chats/{roomId}/members")
    public ResponseEntity<?> addMembers(@PathVariable Long roomId,
                                        @RequestBody Map<String, List<String>> payload,
                                        Principal principal) {
        List<String> usernames = payload.get("usernames");
        if (usernames == null || usernames.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Usernames list cannot be empty"));
        }
        try {
            chatService.addMembersToGroup(roomId, usernames, principal.getName());
            return ResponseEntity.ok(Map.of("success", true, "message", "Members added successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/chats/{roomId}/read")
    public ResponseEntity<?> markRead(@PathVariable Long roomId, Principal principal) {
        chatService.markRoomAsRead(roomId, principal.getName());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/chats/{roomId}/message")
    public ResponseEntity<?> sendMessageRest(@PathVariable Long roomId,
                                             @RequestBody Map<String, String> payload,
                                             Principal principal) {
        String content = payload.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Message content cannot be empty"));
        }
        MessageDto dto = chatService.saveMessage(roomId, principal.getName(), content.trim(), MessageType.CHAT, null);
        // Also broadcast via WebSocket to room topic
        messagingTemplate.convertAndSend("/topic/room." + roomId, dto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<UserDto>> searchUsers(@RequestParam(value = "q", required = false) String query,
                                                     Principal principal) {
        return ResponseEntity.ok(userService.searchUsers(query, principal.getName()));
    }

    @GetMapping("/users/all")
    public ResponseEntity<List<UserDto>> getAllUsers(Principal principal) {
        return ResponseEntity.ok(userService.getAllOtherUsers(principal.getName()));
    }

    @PostMapping("/users/profile")
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest request, Principal principal) {
        try {
            User updated = userService.updateProfile(principal.getName(), request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully",
                    "user", new UserDto(updated)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}

