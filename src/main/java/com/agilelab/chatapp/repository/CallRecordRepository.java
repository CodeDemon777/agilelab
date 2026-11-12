package com.agilelab.chatapp.repository;

import com.agilelab.chatapp.model.CallRecord;
import com.agilelab.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {
    Optional<CallRecord> findByRoomId(String roomId);

    @Query("SELECT c FROM CallRecord c WHERE c.caller = :user OR c.receiver = :user ORDER BY c.startedAt DESC")
    List<CallRecord> findRecentCallsForUser(@Param("user") User user);
}
