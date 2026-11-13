package com.agilelab.chatapp.dto;

import java.util.HashMap;
import java.util.Map;

public class WorkspaceInsightsDto {
    private long totalMessages;
    private long totalRooms;
    private long totalActionItems;
    private long pendingActionItems;
    private long inProgressActionItems;
    private long completedActionItems;
    private long totalMeetingSummaries;
    private double taskCompletionRate;
    private Map<String, Long> userContributions = new HashMap<>();

    public WorkspaceInsightsDto() {
    }

    public long getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(long totalMessages) {
        this.totalMessages = totalMessages;
    }

    public long getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(long totalRooms) {
        this.totalRooms = totalRooms;
    }

    public long getTotalActionItems() {
        return totalActionItems;
    }

    public void setTotalActionItems(long totalActionItems) {
        this.totalActionItems = totalActionItems;
    }

    public long getPendingActionItems() {
        return pendingActionItems;
    }

    public void setPendingActionItems(long pendingActionItems) {
        this.pendingActionItems = pendingActionItems;
    }

    public long getInProgressActionItems() {
        return inProgressActionItems;
    }

    public void setInProgressActionItems(long inProgressActionItems) {
        this.inProgressActionItems = inProgressActionItems;
    }

    public long getCompletedActionItems() {
        return completedActionItems;
    }

    public void setCompletedActionItems(long completedActionItems) {
        this.completedActionItems = completedActionItems;
    }

    public long getTotalMeetingSummaries() {
        return totalMeetingSummaries;
    }

    public void setTotalMeetingSummaries(long totalMeetingSummaries) {
        this.totalMeetingSummaries = totalMeetingSummaries;
    }

    public double getTaskCompletionRate() {
        return taskCompletionRate;
    }

    public void setTaskCompletionRate(double taskCompletionRate) {
        this.taskCompletionRate = taskCompletionRate;
    }

    public Map<String, Long> getUserContributions() {
        return userContributions;
    }

    public void setUserContributions(Map<String, Long> userContributions) {
        this.userContributions = userContributions;
    }
}

