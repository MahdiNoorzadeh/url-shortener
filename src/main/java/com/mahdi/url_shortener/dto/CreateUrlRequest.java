package com.mahdi.url_shortener.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateUrlRequest(
         @NotBlank(message = "URL cannot be blank")
    @Pattern(
        regexp = "^(https?://)?([\\w\\.-]+)\\.([a-z\\.]{2,6})([/\\w .-]*)*/?$",
        message = "Invalid URL format"
    )
    String url,

    OffsetDateTime expiresAt
) {
}
