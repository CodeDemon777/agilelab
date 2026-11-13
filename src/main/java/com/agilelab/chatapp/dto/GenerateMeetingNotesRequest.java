package com.agilelab.chatapp.dto;

public class GenerateMeetingNotesRequest {
    private String roomId;
    private String title;
    private String transcript;

    public GenerateMeetingNotesRequest() {
    }

    public GenerateMeetingNotesRequest(String roomId, String title, String transcript) {
        this.roomId = roomId;
        this.title = title;
        this.transcript = transcript;
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
}

