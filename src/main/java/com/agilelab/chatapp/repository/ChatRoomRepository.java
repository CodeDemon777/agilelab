package com.agilelab.chatapp.repository;

import com.agilelab.chatapp.model.ChatRoom;
import com.agilelab.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT r FROM ChatRoom r JOIN r.participants p WHERE p.user = :user ORDER BY r.createdAt DESC")
    List<ChatRoom> findRoomsByUser(@Param("user") User user);

    @Query("SELECT r FROM ChatRoom r " +
           "WHERE r.group = false " +
           "AND EXISTS (SELECT p1 FROM ChatParticipant p1 WHERE p1.chatRoom = r AND p1.user = :user1) " +
           "AND EXISTS (SELECT p2 FROM ChatParticipant p2 WHERE p2.chatRoom = r AND p2.user = :user2)")
    Optional<ChatRoom> findDirectRoomBetween(@Param("user1") User user1, @Param("user2") User user2);
}

