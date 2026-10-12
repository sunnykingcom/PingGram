package com.pinggram.controller;

import com.pinggram.dto.*;
import com.pinggram.exception.ResourceNotFoundException;
import com.pinggram.repository.UserRepository;
import com.pinggram.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ChatDto>> listChats(@AuthenticationPrincipal UserDetails details) {
        Long userId = resolveId(details.getUsername());
        return ResponseEntity.ok(chatService.getChatsForUser(userId));
    }

    @PostMapping
    public ResponseEntity<ChatDto> createChat(
        @AuthenticationPrincipal UserDetails details,
        @Valid @RequestBody CreateChatRequest req
    ) {
        Long userId = resolveId(details.getUsername());
        ChatDto chat = chatService.createDirectChat(userId, req.recipientEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(chat);
    }

    @PostMapping("/{chatId}/read")
    public ResponseEntity<Void> markRead(
        @PathVariable Long chatId,
        @AuthenticationPrincipal UserDetails details
    ) {
        Long userId = resolveId(details.getUsername());
        if (!chatService.isMember(chatId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        chatService.markAllRead(chatId, userId);
        return ResponseEntity.ok().build();
    }

    private Long resolveId(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"))
            .getId();
    }
}
