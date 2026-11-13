package com.agilelab.chatapp.dto;

public class CitationDto {
    private String senderName;
    private String formattedTime;
    private String messageSnippet;
    private Long roomId;
    private String roomName;

    public CitationDto() {
    }

    public CitationDto(String senderName, String formattedTime, String messageSnippet, Long roomId, String roomName) {
        this.senderName = senderName;
        this.formattedTime = formattedTime;
        this.messageSnippet = messageSnippet;
        this.roomId = roomId;
        this.roomName = roomName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    public void setFormattedTime(String formattedTime) {
        this.formattedTime = formattedTime;
    }

    public String getMessageSnippet() {
        return messageSnippet;
    }

    public void setMessageSnippet(String messageSnippet) {
        this.messageSnippet = messageSnippet;
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
}

