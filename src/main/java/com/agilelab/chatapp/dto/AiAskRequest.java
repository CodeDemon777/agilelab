package com.agilelab.chatapp.dto;

public class AiAskRequest {
    private String query;
    private Long roomId;

    public AiAskRequest() {
    }

    public AiAskRequest(String query, Long roomId) {
        this.query = query;
        this.roomId = roomId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}

