package com.agilelab.chatapp.service;

import com.agilelab.chatapp.model.CallRecord;
import com.agilelab.chatapp.model.NotificationType;
import com.agilelab.chatapp.model.User;
import com.agilelab.chatapp.repository.CallRecordRepository;
import com.agilelab.chatapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CallService {

    private final CallRecordRepository callRecordRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CallService(CallRecordRepository callRecordRepository,
                       UserRepository userRepository,
                       NotificationService notificationService) {
        this.callRecordRepository = callRecordRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public CallRecord initiateCall(String roomId, String callerUsername, String receiverUsername, String callType) {
        User caller = userRepository.findByUsernameIgnoreCase(callerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Caller not found: " + callerUsername));
        User receiver = userRepository.findByUsernameIgnoreCase(receiverUsername)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found: " + receiverUsername));

        CallRecord record = new CallRecord(roomId, caller, receiver, callType != null ? callType : "VIDEO", "RINGING");
        CallRecord saved = callRecordRepository.save(record);

        // Send a call notification to receiver
        notificationService.createNotification(
                receiver.getUsername(),
                caller.getUsername(),
                "Incoming " + callType + " Call",
                caller.getFullName() + " is calling you...",
                "/call/" + roomId + "?target=" + caller.getUsername() + "&type=" + callType,
                NotificationType.CALL
        );

        return saved;
    }

    public void updateCallStatus(String roomId, String status) {
        callRecordRepository.findByRoomId(roomId).ifPresent(record -> {
            record.setStatus(status);
            if ("ENDED".equalsIgnoreCase(status) || "DECLINED".equalsIgnoreCase(status) || "MISSED".equalsIgnoreCase(status)) {
                record.setEndedAt(LocalDateTime.now());
            }
            callRecordRepository.save(record);
        });
    }

    @Transactional(readOnly = true)
    public List<CallRecord> getUserCallHistory(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return callRecordRepository.findRecentCallsForUser(user);
    }
}

