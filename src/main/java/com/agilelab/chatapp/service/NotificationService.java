package com.agilelab.chatapp.service;

import com.agilelab.chatapp.dto.NotificationDto;
import com.agilelab.chatapp.model.Notification;
import com.agilelab.chatapp.model.NotificationType;
import com.agilelab.chatapp.model.User;
import com.agilelab.chatapp.repository.NotificationRepository;
import com.agilelab.chatapp.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Notification createNotification(String recipientUsername, String senderUsername,
                                           String title, String message, String linkUrl,
                                           NotificationType type) {
        User recipient = userRepository.findByUsernameIgnoreCase(recipientUsername).orElse(null);
        if (recipient == null) {
            return null;
        }

        User sender = null;
        if (senderUsername != null) {
            sender = userRepository.findByUsernameIgnoreCase(senderUsername).orElse(null);
        }

        Notification notification = new Notification(recipient, sender, title, message, linkUrl, type);
        Notification saved = notificationRepository.save(notification);

        // Push real-time notification to user's private queue
        NotificationDto dto = new NotificationDto(saved);
        messagingTemplate.convertAndSendToUser(recipient.getUsername(), "/queue/notifications", dto);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user).stream()
                .map(NotificationDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null) return 0;
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    public void markAsRead(Long id, String username) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getRecipient().getUsername().equalsIgnoreCase(username)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    public void markAllAsRead(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (user != null) {
            List<Notification> list = notificationRepository.findByRecipientOrderByCreatedAtDesc(user);
            for (Notification n : list) {
                n.setRead(true);
            }
            notificationRepository.saveAll(list);
        }
    }
}

