package com.pinggram.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id", "user_id"}),
    indexes = {
        @Index(name = "idx_cm_user", columnList = "user_id"),
        @Index(name = "idx_cm_chat", columnList = "chat_id")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.MEMBER;

    /** Timestamp of the last message the member has read */
    private LocalDateTime lastReadAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    public enum Role { OWNER, ADMIN, MEMBER }
}
