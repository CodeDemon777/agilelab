package com.agilelab.chatapp.repository;

import com.agilelab.chatapp.model.ChatParticipant;
import com.agilelab.chatapp.model.ChatRoom;
import com.agilelab.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    List<ChatParticipant> findByChatRoom(ChatRoom chatRoom);
    Optional<ChatParticipant> findByChatRoomAndUser(ChatRoom chatRoom, User user);
    boolean existsByChatRoomAndUser(ChatRoom chatRoom, User user);
}

