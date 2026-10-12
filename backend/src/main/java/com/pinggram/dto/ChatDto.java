package com.pinggram.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ChatDto(
    Long id,
    String name,
    boolean isGroup,
    boolean online,
    List<UserDto> members,
    MessageDto lastMessage,
    long unreadCount,
    LocalDateTime createdAt
) {}
