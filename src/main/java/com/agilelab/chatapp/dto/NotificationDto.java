package com.agilelab.chatapp.dto;

import com.agilelab.chatapp.model.Notification;
import com.agilelab.chatapp.model.NotificationType;
import java.time.format.DateTimeFormatter;

public class NotificationDto {
    private Long id;
    private String title;
    private String message;
    private String linkUrl;
    private NotificationType type;
    private boolean read;
    private String createdAt;
    private String senderUsername;
    private String senderAvatar;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    public NotificationDto() {}

    public NotificationDto(Notification n) {
        if (n != null) {
            this.id = n.getId();
            this.title = n.getTitle();
            this.message = n.getMessage();
            this.linkUrl = n.getLinkUrl();
            this.type = n.getType();
            this.read = n.isRead();
            if (n.getCreatedAt() != null) {
                this.createdAt = n.getCreatedAt().format(FORMATTER);
            }
            if (n.getSender() != null) {
                this.senderUsername = n.getSender().getUsername();
                this.senderAvatar = n.getSender().getAvatarUrl();
            }
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
}

