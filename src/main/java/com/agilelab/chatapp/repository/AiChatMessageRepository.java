package com.agilelab.chatapp.repository;

import com.agilelab.chatapp.model.AiChatMessage;
import com.agilelab.chatapp.model.ChatRoom;
import com.agilelab.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {
    List<AiChatMessage> findByUserOrderByCreatedAtAsc(User user);
    List<AiChatMessage> findByUserAndChatRoomOrderByCreatedAtAsc(User user, ChatRoom chatRoom);
    void deleteByUser(User user);
}

