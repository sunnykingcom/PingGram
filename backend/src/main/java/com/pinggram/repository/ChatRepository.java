package com.pinggram.repository;

import com.pinggram.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    /** All chats the given user belongs to, newest last-message first */
    @Query("""
        SELECT DISTINCT c FROM Chat c
        JOIN c.members m
        WHERE m.user.id = :userId
        ORDER BY c.createdAt DESC
        """)
    List<Chat> findAllByMemberUserId(@Param("userId") Long userId);

    /** Find an existing 1-to-1 chat between two users */
    @Query("""
        SELECT c FROM Chat c
        JOIN c.members m1 ON m1.user.id = :userA
        JOIN c.members m2 ON m2.user.id = :userB
        WHERE c.isGroup = false
        """)
    Optional<Chat> findDirectChat(@Param("userA") Long userA, @Param("userB") Long userB);
}
