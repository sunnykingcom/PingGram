package com.pinggram.service;

import com.pinggram.dto.*;
import com.pinggram.entity.*;
import com.pinggram.exception.ResourceNotFoundException;
import com.pinggram.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository    chatRepository;
    private final UserRepository    userRepository;
    private final SimpMessagingTemplate broker;

    /**
     * Send a message with idempotency: if uniqueId already exists, return the
     * existing record without creating a duplicate.
     */
    @Retryable(retryFor = Exception.class, maxAttempts = 3,
               backoff = @Backoff(delay = 200, multiplier = 2))
    @Transactional
    public MessageDto sendMessage(Long chatId, Long senderId, SendMessageRequest req) {
        // Idempotency check
        return messageRepository.findByUniqueId(req.uniqueId())
            .map(MessageDto::from)
            .orElseGet(() -> {
                Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));
                User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Message msg = Message.builder()
                    .uniqueId(req.uniqueId())
                    .chat(chat)
                    .sender(sender)
                    .content(req.content())
                    .attachmentUrl(req.attachmentUrl())
                    .status(Message.Status.SENT)
                    .build();
                messageRepository.save(msg);

                MessageDto dto = MessageDto.from(msg);
                // Broadcast to all subscribers of this chat topic (real-time)
                broker.convertAndSend("/topic/chat." + chatId, dto);
                // Deliver to recipient user queues for cross-server support
                deliverToRecipients(chat, senderId, dto);
                return dto;
            });
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(Long chatId) {
        return messageRepository.findByChatIdOrderByTimestampAsc(chatId)
            .stream().map(MessageDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getUndeliveredFor(Long userId) {
        return messageRepository.findUndeliveredForUser(userId)
            .stream().map(MessageDto::from).toList();
    }

    @Transactional
    public void markDelivered(Long messageId) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            if (msg.getStatus() == Message.Status.SENT) {
                msg.setStatus(Message.Status.DELIVERED);
                messageRepository.save(msg);
                // Notify sender of delivery receipt
                broker.convertAndSendToUser(
                    msg.getSender().getEmail(),
                    "/queue/receipts",
                    new ReceiptDto(msg.getId(), "DELIVERED")
                );
            }
        });
    }

    @Transactional
    public void markRead(Long messageId) {
        messageRepository.findById(messageId).ifPresent(msg -> {
            msg.setStatus(Message.Status.READ);
            messageRepository.save(msg);
            broker.convertAndSendToUser(
                msg.getSender().getEmail(),
                "/queue/receipts",
                new ReceiptDto(msg.getId(), "READ")
            );
        });
    }

    // ── private helpers ──────────────────────────────
    private void deliverToRecipients(Chat chat, Long senderId, MessageDto dto) {
        chat.getMembers().stream()
            .filter(cm -> !cm.getUser().getId().equals(senderId))
            .forEach(cm -> {
                String recipientEmail = cm.getUser().getEmail();
                broker.convertAndSendToUser(recipientEmail, "/queue/messages", dto);
            });
    }
}
