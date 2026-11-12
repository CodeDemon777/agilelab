package com.agilelab.chatapp.controller;

import com.agilelab.chatapp.dto.CallSignalDto;
import com.agilelab.chatapp.dto.MessageDto;
import com.agilelab.chatapp.model.MessageType;
import com.agilelab.chatapp.model.User;
import com.agilelab.chatapp.service.CallService;
import com.agilelab.chatapp.service.ChatService;
import com.agilelab.chatapp.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserService userService;
    private final CallService callService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService,
                                   UserService userService,
                                   CallService callService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userService = userService;
        this.callService = callService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;

        Long roomId = Long.valueOf(payload.get("roomId").toString());
        String content = (String) payload.get("content");
        String typeStr = (String) payload.getOrDefault("type", "CHAT");
        String fileUrl = (String) payload.get("fileUrl");

        MessageType type = MessageType.CHAT;
        try {
            if (typeStr != null) type = MessageType.valueOf(typeStr);
        } catch (Exception ignored) {}

        MessageDto savedDto = chatService.saveMessage(roomId, principal.getName(), content, type, fileUrl);
        // Broadcast to everyone in the room topic
        messagingTemplate.convertAndSend("/topic/room." + roomId, savedDto);
    }

    @MessageMapping("/chat.typing")
    public void typingIndicator(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) return;

        Long roomId = Long.valueOf(payload.get("roomId").toString());
        boolean isTyping = Boolean.parseBoolean(payload.getOrDefault("isTyping", "false").toString());

        messagingTemplate.convertAndSend("/topic/room." + roomId + ".typing", Map.of(
                "roomId", roomId,
                "username", principal.getName(),
                "isTyping", isTyping
        ));
    }

    @MessageMapping("/call.signal")
    public void handleCallSignal(@Payload CallSignalDto signal, Principal principal) {
        if (principal == null || signal == null) return;

        signal.setFromUser(principal.getName());

        try {
            User sender = userService.findByUsername(principal.getName());
            signal.setFromFullName(sender.getFullName());
            signal.setFromAvatar(sender.getAvatarUrl());
        } catch (Exception ignored) {}

        String type = signal.getType();
        if ("INVITE".equalsIgnoreCase(type) && signal.getToUser() != null) {
            callService.initiateCall(signal.getRoomId(), signal.getFromUser(), signal.getToUser(), signal.getCallType());
        } else if ("ACCEPT".equalsIgnoreCase(type)) {
            callService.updateCallStatus(signal.getRoomId(), "CONNECTED");
        } else if ("DECLINE".equalsIgnoreCase(type)) {
            callService.updateCallStatus(signal.getRoomId(), "DECLINED");
        } else if ("LEAVE".equalsIgnoreCase(type)) {
            callService.updateCallStatus(signal.getRoomId(), "ENDED");
        }

        if (signal.getToUser() != null && !signal.getToUser().isBlank()) {
            messagingTemplate.convertAndSendToUser(signal.getToUser(), "/queue/call", signal);
        } else if (signal.getRoomId() != null) {
            messagingTemplate.convertAndSend("/topic/call." + signal.getRoomId(), signal);
        }
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headers.getUser();
        if (principal != null) {
            userService.setOnlineStatus(principal.getName(), true);
            messagingTemplate.convertAndSend("/topic/presence", Map.of(
                    "username", principal.getName(),
                    "status", "ONLINE"
            ));
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headers.getUser();
        if (principal != null) {
            userService.setOnlineStatus(principal.getName(), false);
            messagingTemplate.convertAndSend("/topic/presence", Map.of(
                    "username", principal.getName(),
                    "status", "OFFLINE"
            ));
        }
    }
}
