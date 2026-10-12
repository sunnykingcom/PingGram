package com.pinggram.dto;

import com.pinggram.entity.Message;

import java.time.LocalDateTime;

public record MessageDto(
    Long id,
    String uniqueId,
    Long chatId,
    Long senderId,
    String senderName,
    String content,
    String attachmentUrl,
    String status,
    LocalDateTime timestamp
) {
    public static MessageDto from(Message m) {
        return new MessageDto(
            m.getId(),
            m.getUniqueId(),
            m.getChat().getId(),
            m.getSender().getId(),
            m.getSender().getDisplayName(),
            m.getContent(),
            m.getAttachmentUrl(),
            m.getStatus().name(),
            m.getTimestamp()
        );
    }
}
