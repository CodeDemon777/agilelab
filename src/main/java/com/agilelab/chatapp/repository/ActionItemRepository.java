package com.agilelab.chatapp.repository;

import com.agilelab.chatapp.model.ActionItem;
import com.agilelab.chatapp.model.ActionItemStatus;
import com.agilelab.chatapp.model.ChatRoom;
import com.agilelab.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    List<ActionItem> findAllByOrderByCreatedAtDesc();
    List<ActionItem> findByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);
    List<ActionItem> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);
    List<ActionItem> findByAssignedToOrderByCreatedAtDesc(User assignedTo);
    List<ActionItem> findByStatusOrderByCreatedAtDesc(ActionItemStatus status);
    List<ActionItem> findByChatRoomIdAndStatusOrderByCreatedAtDesc(Long chatRoomId, ActionItemStatus status);
    long countByStatus(ActionItemStatus status);
}

