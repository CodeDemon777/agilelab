package com.agilelab.chatapp.repository;

import com.agilelab.chatapp.model.ChatMessage;
import com.agilelab.chatapp.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatRoomOrderByTimestampAsc(ChatRoom chatRoom);
    List<ChatMessage> findTop50ByChatRoomOrderByTimestampDesc(ChatRoom chatRoom);
}
