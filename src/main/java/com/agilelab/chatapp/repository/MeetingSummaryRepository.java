package com.agilelab.chatapp.repository;

import com.agilelab.chatapp.model.MeetingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, Long> {
    List<MeetingSummary> findAllByOrderByCreatedAtDesc();
    List<MeetingSummary> findByRoomIdOrderByCreatedAtDesc(String roomId);
    List<MeetingSummary> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);
}

