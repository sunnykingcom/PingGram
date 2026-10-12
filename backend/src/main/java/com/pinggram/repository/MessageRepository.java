package com.pinggram.repository;

import com.pinggram.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findByUniqueId(String uniqueId);

    List<Message> findByChatIdOrderByTimestampAsc(Long chatId);

    /** Undelivered messages where the recipient is a member of the chat and not the sender */
    @Query("""
        SELECT m FROM Message m
        JOIN m.chat c
        JOIN c.members cm
        WHERE cm.user.id = :userId
          AND m.sender.id <> :userId
          AND m.status = com.pinggram.entity.Message.Status.SENT
        ORDER BY m.timestamp ASC
        """)
    List<Message> findUndeliveredForUser(@Param("userId") Long userId);
}
