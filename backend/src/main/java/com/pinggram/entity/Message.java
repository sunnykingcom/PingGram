package com.pinggram.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_msg_chat_ts",  columnList = "chat_id, timestamp"),
    @Index(name = "idx_msg_sender",   columnList = "sender_id"),
    @Index(name = "idx_msg_unique_id", columnList = "unique_id", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Client-generated UUID for idempotency – prevents duplicate inserts on retry */
    @Column(name = "unique_id", nullable = false, unique = true, length = 36)
    private String uniqueId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** URL of an attached file/image (nullable) */
    @Column(length = 2048)
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.SENT;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;

    public enum Status { SENT, DELIVERED, READ }
}
