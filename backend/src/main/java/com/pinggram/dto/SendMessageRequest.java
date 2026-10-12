package com.pinggram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
    @NotBlank @Size(max = 4096) String content,
    @NotNull String uniqueId,
    String attachmentUrl
) {}
