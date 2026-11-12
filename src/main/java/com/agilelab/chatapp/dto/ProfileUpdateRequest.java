package com.agilelab.chatapp.dto;

public class ProfileUpdateRequest {
    private String fullName;
    private String email;
    private String statusQuote;
    private String avatarUrl;
    private String currentPassword;
    private String newPassword;

    public ProfileUpdateRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatusQuote() { return statusQuote; }
    public void setStatusQuote(String statusQuote) { this.statusQuote = statusQuote; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}

