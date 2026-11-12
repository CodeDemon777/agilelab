package com.agilelab.chatapp.dto;

import com.agilelab.chatapp.model.User;
import java.time.LocalDateTime;

public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String statusQuote;
    private boolean online;
    private LocalDateTime lastSeen;

    public UserDto() {}

    public UserDto(User user) {
        if (user != null) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.fullName = user.getFullName();
            this.avatarUrl = user.getAvatarUrl();
            this.statusQuote = user.getStatusQuote();
            this.online = user.isOnline();
            this.lastSeen = user.getLastSeen();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getStatusQuote() { return statusQuote; }
    public void setStatusQuote(String statusQuote) { this.statusQuote = statusQuote; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
}
