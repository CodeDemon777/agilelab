package com.agilelab.chatapp.service;

import com.agilelab.chatapp.dto.ChatRoomDto;
import com.agilelab.chatapp.dto.CreateGroupRequest;
import com.agilelab.chatapp.dto.MessageDto;
import com.agilelab.chatapp.dto.UserDto;
import com.agilelab.chatapp.model.*;
import com.agilelab.chatapp.repository.ChatMessageRepository;
import com.agilelab.chatapp.repository.ChatParticipantRepository;
import com.agilelab.chatapp.repository.ChatRoomRepository;
import com.agilelab.chatapp.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    public ChatService(ChatRoomRepository chatRoomRepository,
                       ChatParticipantRepository participantRepository,
                       ChatMessageRepository messageRepository,
                       UserRepository userRepository,
                       NotificationService notificationService) {
        this.chatRoomRepository = chatRoomRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDto> getUserChatRooms(String currentUsername) {
        User currentUser = userRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

        List<ChatRoom> rooms = chatRoomRepository.findRoomsByUser(currentUser);
        List<ChatRoomDto> dtos = new ArrayList<>();

        for (ChatRoom room : rooms) {
            dtos.add(toChatRoomDto(room, currentUser));
        }

        // Sort by latest message or room creation time descending
        dtos.sort((a, b) -> {
            String timeA = a.getLastMessageTime();
            String timeB = b.getLastMessageTime();
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return b.getId().compareTo(a.getId());
        });

        return dtos;
    }

    @Transactional(readOnly = true)
    public ChatRoomDto getChatRoomDetails(Long roomId, String currentUsername) {
        User currentUser = userRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));

        boolean isMember = participantRepository.existsByChatRoomAndUser(room, currentUser);
        if (!isMember) {
            throw new SecurityException("User is not a member of this chat room");
        }

        return toChatRoomDto(room, currentUser);
    }

    public ChatRoom getOrCreateDirectRoom(String currentUsername, String targetUsername) {
        if (currentUsername.equalsIgnoreCase(targetUsername)) {
            throw new IllegalArgumentException("Cannot create a direct chat with yourself.");
        }

        User user1 = userRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));
        User user2 = userRepository.findByUsernameIgnoreCase(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + targetUsername));

        Optional<ChatRoom> existing = chatRoomRepository.findDirectRoomBetween(user1, user2);
        if (existing.isPresent()) {
            return existing.get();
        }

        ChatRoom newRoom = new ChatRoom(null, false);
        ChatRoom savedRoom = chatRoomRepository.save(newRoom);

        ChatParticipant p1 = new ChatParticipant(savedRoom, user1);
        ChatParticipant p2 = new ChatParticipant(savedRoom, user2);
        participantRepository.save(p1);
        participantRepository.save(p2);

        savedRoom.getParticipants().add(p1);
        savedRoom.getParticipants().add(p2);

        return savedRoom;
    }

    public ChatRoom createGroupRoom(String currentUsername, CreateGroupRequest req) {
        User creator = userRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

        ChatRoom room = new ChatRoom(req.getName().trim(), true);
        ChatRoom saved = chatRoomRepository.save(room);

        ChatParticipant creatorPart = new ChatParticipant(saved, creator);
        participantRepository.save(creatorPart);
        saved.getParticipants().add(creatorPart);

        if (req.getMemberUsernames() != null) {
            for (String username : req.getMemberUsernames()) {
                if (!username.equalsIgnoreCase(currentUsername)) {
                    userRepository.findByUsernameIgnoreCase(username).ifPresent(user -> {
                        ChatParticipant p = new ChatParticipant(saved, user);
                        participantRepository.save(p);
                        saved.getParticipants().add(p);

                        // notify member of being added
                        notificationService.createNotification(
                                user.getUsername(),
                                creator.getUsername(),
                                "Added to Group",
                                creator.getFullName() + " added you to the group '" + saved.getName() + "'.",
                                "/chats?room=" + saved.getId(),
                                NotificationType.GROUP_INVITE
                        );
                    });
                }
            }
        }

        // Add a system welcome message
        ChatMessage welcomeMsg = new ChatMessage(saved, creator,
                creator.getFullName() + " created group \"" + req.getName() + "\"",
                MessageType.JOIN);
        messageRepository.save(welcomeMsg);

        return saved;
    }

    public void addMembersToGroup(Long roomId, List<String> usernames, String currentUsername) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found"));
        if (!room.isGroup()) {
            throw new IllegalArgumentException("Cannot add members to a 1-on-1 chat.");
        }

        User adder = userRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

        for (String uname : usernames) {
            userRepository.findByUsernameIgnoreCase(uname).ifPresent(user -> {
                if (!participantRepository.existsByChatRoomAndUser(room, user)) {
                    ChatParticipant p = new ChatParticipant(room, user);
                    participantRepository.save(p);
                    room.getParticipants().add(p);

                    ChatMessage joinMsg = new ChatMessage(room, user,
                            user.getFullName() + " was added to the group",
                            MessageType.JOIN);
                    messageRepository.save(joinMsg);

                    notificationService.createNotification(
                            user.getUsername(),
                            adder.getUsername(),
                            "Added to Group",
                            adder.getFullName() + " added you to the group '" + room.getName() + "'.",
                            "/chats?room=" + room.getId(),
                            NotificationType.GROUP_INVITE
                    );
                }
            });
        }
    }

    public MessageDto saveMessage(Long roomId, String senderUsername, String content, MessageType type, String fileUrl) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
        User sender = userRepository.findByUsernameIgnoreCase(senderUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + senderUsername));

        boolean isMember = participantRepository.existsByChatRoomAndUser(room, sender);
        if (!isMember) {
            throw new SecurityException("User '" + senderUsername + "' is not a member of chat room " + roomId);
        }

        ChatMessage message = new ChatMessage(room, sender, content, type != null ? type : MessageType.CHAT);
        message.setFileUrl(fileUrl);
        ChatMessage saved = messageRepository.save(message);

        // Notify other participants
        List<ChatParticipant> participants = participantRepository.findByChatRoom(room);
        for (ChatParticipant p : participants) {
            if (!p.getUser().getUsername().equalsIgnoreCase(senderUsername)) {
                String title = room.isGroup() ? room.getName() : sender.getFullName();
                String preview = content.length() > 40 ? content.substring(0, 37) + "..." : content;
                notificationService.createNotification(
                        p.getUser().getUsername(),
                        sender.getUsername(),
                        "New message from " + title,
                        preview,
                        "/chats?room=" + room.getId(),
                        NotificationType.MESSAGE
                );
            }
        }

        return new MessageDto(saved);
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getRoomMessages(Long roomId, String currentUsername) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
        User user = userRepository.findByUsernameIgnoreCase(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

        if (!participantRepository.existsByChatRoomAndUser(room, user)) {
            throw new SecurityException("User is not authorized to view messages in this room.");
        }

        List<ChatMessage> list = messageRepository.findByChatRoomOrderByTimestampAsc(room);
        return list.stream().map(MessageDto::new).collect(Collectors.toList());
    }

    public void markRoomAsRead(Long roomId, String currentUsername) {
        ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
        User user = userRepository.findByUsernameIgnoreCase(currentUsername).orElse(null);
        if (room != null && user != null) {
            participantRepository.findByChatRoomAndUser(room, user).ifPresent(p -> {
                p.setLastReadAt(LocalDateTime.now());
                participantRepository.save(p);
            });
        }
    }

    private ChatRoomDto toChatRoomDto(ChatRoom room, User currentUser) {
        ChatRoomDto dto = new ChatRoomDto();
        dto.setId(room.getId());
        dto.setGroup(room.isGroup());

        List<ChatParticipant> participants = participantRepository.findByChatRoom(room);
        List<UserDto> memberDtos = participants.stream()
                .map(p -> new UserDto(p.getUser()))
                .collect(Collectors.toList());
        dto.setMembers(memberDtos);

        if (room.isGroup()) {
            dto.setDisplayName(room.getName());
            dto.setAvatarUrl("https://api.dicebear.com/7.x/identicon/svg?seed=" + (room.getName() != null ? room.getName() : "group"));
            dto.setOnline(memberDtos.stream().anyMatch(m -> !m.getUsername().equalsIgnoreCase(currentUser.getUsername()) && m.isOnline()));
        } else {
            // 1-on-1 chat: find the other participant
            User partner = participants.stream()
                    .map(ChatParticipant::getUser)
                    .filter(u -> !u.getUsername().equalsIgnoreCase(currentUser.getUsername()))
                    .findFirst()
                    .orElse(currentUser);

            dto.setDisplayName(partner.getFullName());
            dto.setAvatarUrl(partner.getAvatarUrl());
            dto.setOnline(partner.isOnline());
            dto.setPartnerUsername(partner.getUsername());
        }

        // Get latest message
        List<ChatMessage> messages = messageRepository.findByChatRoomOrderByTimestampAsc(room);
        if (!messages.isEmpty()) {
            ChatMessage latest = messages.get(messages.size() - 1);
            dto.setLastMessagePreview(latest.getContent());
            dto.setLastMessageTime(latest.getTimestamp().format(TIME_FORMATTER));
        } else {
            dto.setLastMessagePreview("No messages yet");
            dto.setLastMessageTime("");
        }

        return dto;
    }
}

