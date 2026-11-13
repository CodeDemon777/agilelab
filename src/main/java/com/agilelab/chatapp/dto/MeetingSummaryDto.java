package com.agilelab.chatapp.dto;

import com.agilelab.chatapp.model.MeetingSummary;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MeetingSummaryDto {
    private Long id;
    private String roomId;
    private String title;
    private String transcript;
    private String summaryText;
    private List<String> keyDecisions = new ArrayList<>();
    private List<String> actionItems = new ArrayList<>();
    private String createdByUsername;
    private String createdByName;
    private String formattedCreatedAt;

    public MeetingSummaryDto() {
    }

    public MeetingSummaryDto(MeetingSummary meeting) {
        this.id = meeting.getId();
        this.roomId = meeting.getRoomId();
        this.title = meeting.getTitle();
        this.transcript = meeting.getTranscript();
        this.summaryText = meeting.getSummaryText();

        if (meeting.getKeyDecisions() != null && !meeting.getKeyDecisions().isBlank()) {
            this.keyDecisions = Arrays.stream(meeting.getKeyDecisions().split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        if (meeting.getActionItems() != null && !meeting.getActionItems().isBlank()) {
            this.actionItems = Arrays.stream(meeting.getActionItems().split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }

        if (meeting.getCreatedBy() != null) {
            this.createdByUsername = meeting.getCreatedBy().getUsername();
            this.createdByName = meeting.getCreatedBy().getFullName();
        }
        if (meeting.getCreatedAt() != null) {
            this.formattedCreatedAt = meeting.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public List<String> getKeyDecisions() {
        return keyDecisions;
    }

    public void setKeyDecisions(List<String> keyDecisions) {
        this.keyDecisions = keyDecisions;
    }

    public List<String> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<String> actionItems) {
        this.actionItems = actionItems;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getFormattedCreatedAt() {
        return formattedCreatedAt;
    }

    public void setFormattedCreatedAt(String formattedCreatedAt) {
        this.formattedCreatedAt = formattedCreatedAt;
    }
}

