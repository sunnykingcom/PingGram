package com.pinggram.repository;

import com.pinggram.entity.ChatMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatMemberRepository extends JpaRepository<ChatMember, Long> {
    boolean existsByChatIdAndUserId(Long chatId, Long userId);
    Optional<ChatMember> findByChatIdAndUserId(Long chatId, Long userId);
}
