package com.agilelab.chatapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_records", indexes = {
    @Index(name = "idx_call_room", columnList = "room_id"),
    @Index(name = "idx_call_caller", columnList = "caller_id"),
    @Index(name = "idx_call_receiver", columnList = "receiver_id")
})
public class CallRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "caller_id", nullable = false)
    private User caller;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "call_type", nullable = false, length = 20)
    private String callType; // "VIDEO" or "VOICE"

    @Column(nullable = false, length = 20)
    private String status; // "RINGING", "CONNECTED", "MISSED", "DECLINED", "ENDED"

    @Column(name = "started_at")
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public CallRecord() {
    }

    public CallRecord(String roomId, User caller, User receiver, String callType, String status) {
        this.roomId = roomId;
        this.caller = caller;
        this.receiver = receiver;
        this.callType = callType;
        this.status = status;
        this.startedAt = LocalDateTime.now();
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

    public User getCaller() {
        return caller;
    }

    public void setCaller(User caller) {
        this.caller = caller;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
}

