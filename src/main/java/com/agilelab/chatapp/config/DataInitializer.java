package com.agilelab.chatapp.config;

import com.agilelab.chatapp.model.*;
import com.agilelab.chatapp.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ChatRoomRepository chatRoomRepository,
                           ChatParticipantRepository participantRepository,
                           ChatMessageRepository messageRepository,
                           NotificationRepository notificationRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Already initialized
        }

        // 1. Create Default Users
        User john = new User("johndoe", "johndoe@example.com", passwordEncoder.encode("password123"), "John Doe");
        john.setStatusQuote("Living the dream");
        john.setAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=JohnDoe");
        john.setOnline(true);

        User alice = new User("alice", "alice@example.com", passwordEncoder.encode("password123"), "Alice Smith");
        alice.setStatusQuote("Building great things 🚀");
        alice.setAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=Alice");
        alice.setOnline(true);

        User bob = new User("bob", "bob@example.com", passwordEncoder.encode("password123"), "Bob Wilson");
        bob.setStatusQuote("Available for quick syncs 🎧");
        bob.setAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=Bob");
        bob.setOnline(false);
        bob.setLastSeen(LocalDateTime.now().minusHours(2));

        User emma = new User("emma", "emma@example.com", passwordEncoder.encode("password123"), "Emma Davis");
        emma.setStatusQuote("Coding Spring Boot & WebRTC ✨");
        emma.setAvatarUrl("https://api.dicebear.com/7.x/bottts/svg?seed=Emma");
        emma.setOnline(true);

        userRepository.save(john);
        userRepository.save(alice);
        userRepository.save(bob);
        userRepository.save(emma);

        // 2. Direct Chat: John Doe & Alice Smith
        ChatRoom directJohnAlice = new ChatRoom(null, false);
        chatRoomRepository.save(directJohnAlice);
        participantRepository.save(new ChatParticipant(directJohnAlice, john));
        participantRepository.save(new ChatParticipant(directJohnAlice, alice));

        ChatMessage m1 = new ChatMessage(directJohnAlice, alice, "Hello! How are you?", MessageType.CHAT);
        m1.setTimestamp(LocalDateTime.now().minusMinutes(25));
        ChatMessage m2 = new ChatMessage(directJohnAlice, john, "I'm good, thanks! How about you?", MessageType.CHAT);
        m2.setTimestamp(LocalDateTime.now().minusMinutes(20));
        ChatMessage m3 = new ChatMessage(directJohnAlice, alice, "Working on the Spring Boot agile project. It's looking amazing!", MessageType.CHAT);
        m3.setTimestamp(LocalDateTime.now().minusMinutes(15));
        messageRepository.save(m1);
        messageRepository.save(m2);
        messageRepository.save(m3);

        // 3. Direct Chat: John Doe & Bob Wilson
        ChatRoom directJohnBob = new ChatRoom(null, false);
        chatRoomRepository.save(directJohnBob);
        participantRepository.save(new ChatParticipant(directJohnBob, john));
        participantRepository.save(new ChatParticipant(directJohnBob, bob));

        ChatMessage mb1 = new ChatMessage(directJohnBob, bob, "Hey, how are you?", MessageType.CHAT);
        mb1.setTimestamp(LocalDateTime.now().minusHours(1));
        ChatMessage mb2 = new ChatMessage(directJohnBob, john, "Doing great! Ready for today's sprint review.", MessageType.CHAT);
        mb2.setTimestamp(LocalDateTime.now().minusMinutes(50));
        messageRepository.save(mb1);
        messageRepository.save(mb2);

        // 4. Group Chat: "Group Chat"
        ChatRoom groupChat = new ChatRoom("Group Chat", true);
        chatRoomRepository.save(groupChat);
        participantRepository.save(new ChatParticipant(groupChat, john));
        participantRepository.save(new ChatParticipant(groupChat, alice));
        participantRepository.save(new ChatParticipant(groupChat, bob));
        participantRepository.save(new ChatParticipant(groupChat, emma));

        ChatMessage gm1 = new ChatMessage(groupChat, alice, "Hey everyone! Welcome to Agile Lab team chat.", MessageType.CHAT);
        gm1.setTimestamp(LocalDateTime.now().minusHours(3));
        ChatMessage gm2 = new ChatMessage(groupChat, bob, "Let's meet at 5 PM.", MessageType.CHAT);
        gm2.setTimestamp(LocalDateTime.now().minusHours(2));
        ChatMessage gm3 = new ChatMessage(groupChat, john, "5 PM works for me 👍", MessageType.CHAT);
        gm3.setTimestamp(LocalDateTime.now().minusHours(1));
        ChatMessage gm4 = new ChatMessage(groupChat, emma, "I'll join the video call at 5 PM too!", MessageType.CHAT);
        gm4.setTimestamp(LocalDateTime.now().minusMinutes(30));
        messageRepository.save(gm1);
        messageRepository.save(gm2);
        messageRepository.save(gm3);
        messageRepository.save(gm4);

        // 5. Default Notifications for John Doe
        Notification n1 = new Notification(john, alice, "New message from Alice Smith",
                "Working on the Spring Boot agile project. It's looking amazing!",
                "/chats?room=" + directJohnAlice.getId(), NotificationType.MESSAGE);
        n1.setCreatedAt(LocalDateTime.now().minusMinutes(15));

        Notification n2 = new Notification(john, bob, "Missed call from Group Chat",
                "Missed team voice sync at 4:30 PM",
                "/call/" + groupChat.getId() + "?target=Group%20Chat&type=VOICE", NotificationType.CALL);
        n2.setCreatedAt(LocalDateTime.now().minusMinutes(45));

        Notification n3 = new Notification(john, alice, "Group Chat Created",
                "Alice Smith added you to Group Chat",
                "/chats?room=" + groupChat.getId(), NotificationType.GROUP_INVITE);
        n3.setCreatedAt(LocalDateTime.now().minusHours(3));

        notificationRepository.save(n1);
        notificationRepository.save(n2);
        notificationRepository.save(n3);
    }
}

