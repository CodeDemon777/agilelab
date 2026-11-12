package com.agilelab.chatapp.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomDto {
    private Long id;
    private String displayName;
    private boolean group;
    private String avatarUrl;
    private boolean online;
    private String lastMessagePreview;
    private String lastMessageTime;
    private int unreadCount;
    private List<UserDto> members = new ArrayList<>();
    private String partnerUsername;

    public ChatRoomDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isGroup() { return group; }
    public void setGroup(boolean group) { this.group = group; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }

    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public List<UserDto> getMembers() { return members; }
    public void setMembers(List<UserDto> members) { this.members = members; }

    public String getPartnerUsername() { return partnerUsername; }
    public void setPartnerUsername(String partnerUsername) { this.partnerUsername = partnerUsername; }
}
