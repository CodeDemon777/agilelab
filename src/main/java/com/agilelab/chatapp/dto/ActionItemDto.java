package com.agilelab.chatapp.dto;

import com.agilelab.chatapp.model.ActionItem;
import com.agilelab.chatapp.model.ActionItemPriority;
import com.agilelab.chatapp.model.ActionItemStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ActionItemDto {
    private Long id;
    private Long roomId;
    private String roomName;
    private String title;
    private String description;
    private String assignedToUsername;
    private String assignedToName;
    private String assignedToAvatar;
    private LocalDate dueDate;
    private String formattedDueDate;
    private ActionItemStatus status;
    private ActionItemPriority priority;
    private String formattedCreatedAt;

    public ActionItemDto() {
    }

    public ActionItemDto(ActionItem item) {
        this.id = item.getId();
        if (item.getChatRoom() != null) {
            this.roomId = item.getChatRoom().getId();
            this.roomName = item.getChatRoom().getName();
        }
        this.title = item.getTitle();
        this.description = item.getDescription();
        if (item.getAssignedTo() != null) {
            this.assignedToUsername = item.getAssignedTo().getUsername();
            this.assignedToName = item.getAssignedTo().getFullName();
            this.assignedToAvatar = item.getAssignedTo().getAvatarUrl();
        }
        this.dueDate = item.getDueDate();
        if (item.getDueDate() != null) {
            this.formattedDueDate = item.getDueDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }
        this.status = item.getStatus();
        this.priority = item.getPriority();
        if (item.getCreatedAt() != null) {
            this.formattedCreatedAt = item.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignedToUsername() {
        return assignedToUsername;
    }

    public void setAssignedToUsername(String assignedToUsername) {
        this.assignedToUsername = assignedToUsername;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public String getAssignedToAvatar() {
        return assignedToAvatar;
    }

    public void setAssignedToAvatar(String assignedToAvatar) {
        this.assignedToAvatar = assignedToAvatar;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getFormattedDueDate() {
        return formattedDueDate;
    }

    public void setFormattedDueDate(String formattedDueDate) {
        this.formattedDueDate = formattedDueDate;
    }

    public ActionItemStatus getStatus() {
        return status;
    }

    public void setStatus(ActionItemStatus status) {
        this.status = status;
    }

    public ActionItemPriority getPriority() {
        return priority;
    }

    public void setPriority(ActionItemPriority priority) {
        this.priority = priority;
    }

    public String getFormattedCreatedAt() {
        return formattedCreatedAt;
    }

    public void setFormattedCreatedAt(String formattedCreatedAt) {
        this.formattedCreatedAt = formattedCreatedAt;
    }
}

