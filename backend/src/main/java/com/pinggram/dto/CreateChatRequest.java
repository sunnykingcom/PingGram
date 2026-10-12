package com.pinggram.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateChatRequest(
    @NotBlank @Email String recipientEmail
) {}
