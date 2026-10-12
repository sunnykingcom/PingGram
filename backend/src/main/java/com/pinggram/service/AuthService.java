package com.pinggram.service;

import com.pinggram.dto.*;
import com.pinggram.entity.User;
import com.pinggram.exception.ConflictException;
import com.pinggram.exception.ResourceNotFoundException;
import com.pinggram.repository.UserRepository;
import com.pinggram.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered");
        }
        User user = User.builder()
            .displayName(req.displayName())
            .email(req.email())
            .passwordHash(passwordEncoder.encode(req.password()))
            .online(true)
            .build();
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, UserDto.from(user));
    }

    @Transactional
    public AuthResponse login(AuthRequest req) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
        User user = userRepository.findByEmail(req.email())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setOnline(true);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public UserDto getMe(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserDto.from(user);
    }

    @Transactional
    public void setOffline(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setOnline(false);
            userRepository.save(u);
        });
    }
}
