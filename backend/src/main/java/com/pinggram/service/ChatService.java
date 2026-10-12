package com.pinggram.service;

import com.pinggram.dto.*;
import com.pinggram.entity.*;
import com.pinggram.exception.ResourceNotFoundException;
import com.pinggram.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public List<ChatDto> getChatsForUser(Long userId) {
        return chatRepository.findAllByMemberUserId(userId).stream()
            .map(chat -> toDto(chat, userId))
            .toList();
    }

    @Transactional
    public ChatDto createDirectChat(Long requesterId, String recipientEmail) {
        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User recipient = userRepository.findByEmail(recipientEmail)
            .orElseThrow(() -> new ResourceNotFoundException("No user with that email"));

        // Reuse existing direct chat if present
        return chatRepository.findDirectChat(requesterId, recipient.getId())
            .map(existing -> toDto(existing, requesterId))
            .orElseGet(() -> {
                Chat chat = Chat.builder().isGroup(false).build();
                chatRepository.save(chat);
                addMember(chat, requester, ChatMember.Role.MEMBER);
                addMember(chat, recipient, ChatMember.Role.MEMBER);
                return toDto(chat, requesterId);
            });
    }

    @Transactional
    public void markAllRead(Long chatId, Long userId) {
        chatMemberRepository.findByChatIdAndUserId(chatId, userId).ifPresent(cm -> {
            cm.setLastReadAt(LocalDateTime.now());
            chatMemberRepository.save(cm);
        });
        messageRepository.findByChatIdOrderByTimestampAsc(chatId).stream()
            .filter(m -> m.getStatus() != Message.Status.READ
                      && !m.getSender().getId().equals(userId))
            .forEach(m -> {
                m.setStatus(Message.Status.READ);
                messageRepository.save(m);
            });
    }

    public boolean isMember(Long chatId, Long userId) {
        return chatMemberRepository.existsByChatIdAndUserId(chatId, userId);
    }

    // ── internal helpers ──────────────────────────────
    private void addMember(Chat chat, User user, ChatMember.Role role) {
        ChatMember cm = ChatMember.builder().chat(chat).user(user).role(role).build();
        chatMemberRepository.save(cm);
    }

    private ChatDto toDto(Chat chat, Long viewerId) {
        List<UserDto> members = chat.getMembers().stream()
            .map(cm -> UserDto.from(cm.getUser()))
            .toList();

        boolean otherOnline = members.stream()
            .anyMatch(m -> !m.id().equals(viewerId) && m.online());

        Message last = chat.getMessages().isEmpty()
            ? null : chat.getMessages().getLast();

        long unread = chat.getMessages().stream()
            .filter(m -> m.getStatus() != Message.Status.READ
                      && !m.getSender().getId().equals(viewerId))
            .count();

        return new ChatDto(
            chat.getId(),
            chat.getName(),
            chat.isGroup(),
            otherOnline,
            members,
            last != null ? MessageDto.from(last) : null,
            unread,
            chat.getCreatedAt()
        );
    }
}
