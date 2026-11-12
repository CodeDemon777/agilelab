package com.agilelab.chatapp.dto;

public class CallSignalDto {
    private String type; // INVITE, ACCEPT, DECLINE, OFFER, ANSWER, CANDIDATE, LEAVE, BUSY
    private String roomId;
    private String fromUser;
    private String toUser;
    private String callType; // VIDEO or VOICE
    private Object sdp;
    private Object candidate;
    private String fromFullName;
    private String fromAvatar;

    public CallSignalDto() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getFromUser() { return fromUser; }
    public void setFromUser(String fromUser) { this.fromUser = fromUser; }

    public String getToUser() { return toUser; }
    public void setToUser(String toUser) { this.toUser = toUser; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public Object getSdp() { return sdp; }
    public void setSdp(Object sdp) { this.sdp = sdp; }

    public Object getCandidate() { return candidate; }
    public void setCandidate(Object candidate) { this.candidate = candidate; }

    public String getFromFullName() { return fromFullName; }
    public void setFromFullName(String fromFullName) { this.fromFullName = fromFullName; }

    public String getFromAvatar() { return fromAvatar; }
    public void setFromAvatar(String fromAvatar) { this.fromAvatar = avatar(fromAvatar); }

    private String avatar(String a) { return a != null ? a : "/images/default-avatar.svg"; }
}
