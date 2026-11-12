package com.agilelab.chatapp.dto;

import com.agilelab.chatapp.model.ChatMessage;
import com.agilelab.chatapp.model.MessageType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageDto {
    private Long id;
    private Long roomId;
    private String senderUsername;
    private String senderFullName;
    private String senderAvatar;
    private String content;
    private MessageType type;
    private String timestamp;
    private LocalDateTime rawTimestamp;
    private String fileUrl;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    public MessageDto() {}

    public MessageDto(ChatMessage message) {
        if (message != null) {
            this.id = message.getId();
            this.roomId = message.getChatRoom() != null ? message.getChatRoom().getId() : null;
            if (message.getSender() != null) {
                this.senderUsername = message.getSender().getUsername();
                this.senderFullName = message.getSender().getFullName();
                this.senderAvatar = message.getSender().getAvatarUrl();
            }
            this.content = message.getContent();
            this.type = message.getType();
            this.rawTimestamp = message.getTimestamp();
            if (message.getTimestamp() != null) {
                this.timestamp = message.getTimestamp().format(TIME_FORMATTER);
            }
            this.fileUrl = message.getFileUrl();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getSenderFullName() { return senderFullName; }
    public void setSenderFullName(String senderFullName) { this.senderFullName = senderFullName; }

    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public LocalDateTime getRawTimestamp() { return rawTimestamp; }
    public void setRawTimestamp(LocalDateTime rawTimestamp) { this.rawTimestamp = rawTimestamp; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}
