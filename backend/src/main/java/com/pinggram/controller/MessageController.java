package com.pinggram.controller;

import com.pinggram.dto.*;
import com.pinggram.exception.ResourceNotFoundException;
import com.pinggram.repository.UserRepository;
import com.pinggram.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final ChatService    chatService;
    private final UserRepository userRepository;

    /** Fetch message history for a chat */
    @GetMapping("/api/chats/{chatId}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(
        @PathVariable Long chatId,
        @AuthenticationPrincipal UserDetails details
    ) {
        Long userId = resolveId(details.getUsername());
        if (!chatService.isMember(chatId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(messageService.getMessages(chatId));
    }

    /** Send a message (idempotent via uniqueId) */
    @PostMapping("/api/chats/{chatId}/messages")
    public ResponseEntity<MessageDto> sendMessage(
        @PathVariable Long chatId,
        @AuthenticationPrincipal UserDetails details,
        @Valid @RequestBody SendMessageRequest req
    ) {
        Long userId = resolveId(details.getUsername());
        if (!chatService.isMember(chatId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        MessageDto msg = messageService.sendMessage(chatId, userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    /** Pull undelivered messages on reconnect (offline sync) */
    @GetMapping("/api/messages/undelivered")
    public ResponseEntity<List<MessageDto>> getUndelivered(
        @AuthenticationPrincipal UserDetails details
    ) {
        Long userId = resolveId(details.getUsername());
        List<MessageDto> msgs = messageService.getUndeliveredFor(userId);
        // Mark each as delivered now that the user is online
        msgs.forEach(m -> messageService.markDelivered(m.id()));
        return ResponseEntity.ok(msgs);
    }

    private Long resolveId(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"))
            .getId();
    }
}
