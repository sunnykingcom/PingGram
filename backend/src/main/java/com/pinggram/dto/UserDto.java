package com.pinggram.dto;

import com.pinggram.entity.User;

public record UserDto(Long id, String displayName, String email, boolean online) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getDisplayName(), u.getEmail(), u.isOnline());
    }
}
