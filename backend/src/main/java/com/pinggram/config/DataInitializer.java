package com.pinggram.config;

import com.pinggram.entity.*;
import com.pinggram.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository    userRepo;
    private final ChatRepository    chatRepo;
    private final ChatMemberRepository memberRepo;
    private final PasswordEncoder   encoder;

    @Bean
    CommandLineRunner seedTestData() {
        return args -> {
            if (userRepo.existsByEmail("alice@pinggram.com")) {
                log.info("Test data already exists — skipping seed");
                return;
            }

            // ── Users ────────────────────────────────────
            User alice = userRepo.save(User.builder()
                .displayName("Alice Green")
                .email("alice@pinggram.com")
                .passwordHash(encoder.encode("password123"))
                .online(false)
                .build());

            User bob = userRepo.save(User.builder()
                .displayName("Bob Smith")
                .email("bob@pinggram.com")
                .passwordHash(encoder.encode("password123"))
                .online(false)
                .build());

            User carol = userRepo.save(User.builder()
                .displayName("Carol White")
                .email("carol@pinggram.com")
                .passwordHash(encoder.encode("password123"))
                .online(false)
                .build());

            // ── Alice ↔ Bob direct chat ──────────────────
            Chat abChat = chatRepo.save(Chat.builder().isGroup(false).build());
            memberRepo.save(ChatMember.builder().chat(abChat).user(alice).role(ChatMember.Role.MEMBER).build());
            memberRepo.save(ChatMember.builder().chat(abChat).user(bob).role(ChatMember.Role.MEMBER).build());

            // ── Alice ↔ Carol direct chat ────────────────
            Chat acChat = chatRepo.save(Chat.builder().isGroup(false).build());
            memberRepo.save(ChatMember.builder().chat(acChat).user(alice).role(ChatMember.Role.MEMBER).build());
            memberRepo.save(ChatMember.builder().chat(acChat).user(carol).role(ChatMember.Role.MEMBER).build());

            log.info("✓ Test data seeded — 3 users, 2 chats created");
            log.info("  alice@pinggram.com  /  password123");
            log.info("  bob@pinggram.com    /  password123");
            log.info("  carol@pinggram.com  /  password123");
        };
    }
}
