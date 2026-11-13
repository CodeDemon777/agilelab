package com.agilelab.chatapp.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatSummaryDto {
    private Long roomId;
    private String roomName;
    private int messageCount;
    private String executiveSummary;
    private List<String> keyPoints = new ArrayList<>();
    private List<String> decisions = new ArrayList<>();
    private List<String> actionItems = new ArrayList<>();
    private String sentiment;
    private String formattedGeneratedAt;

    public ChatSummaryDto() {
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

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public String getExecutiveSummary() {
        return executiveSummary;
    }

    public void setExecutiveSummary(String executiveSummary) {
        this.executiveSummary = executiveSummary;
    }

    public List<String> getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(List<String> keyPoints) {
        this.keyPoints = keyPoints;
    }

    public List<String> getDecisions() {
        return decisions;
    }

    public void setDecisions(List<String> decisions) {
        this.decisions = decisions;
    }

    public List<String> getActionItems() {
        return actionItems;
    }

    public void setActionItems(List<String> actionItems) {
        this.actionItems = actionItems;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getFormattedGeneratedAt() {
        return formattedGeneratedAt;
    }

    public void setFormattedGeneratedAt(String formattedGeneratedAt) {
        this.formattedGeneratedAt = formattedGeneratedAt;
    }
}

