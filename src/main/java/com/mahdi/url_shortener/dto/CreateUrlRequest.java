package com.mahdi.url_shortener.dto;
import com.mahdi.url_shortener.validation.ValidUrl;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

public record CreateUrlRequest(
        @NotBlank(message = "URL cannot be blank")
    @ValidUrl
    @Schema(
    description = "The original URL to shorten",
    example = "https://example.com"
    )
    String url,
    @Schema(
    description = "Optional expiration date and time for the shortened URL",
    example = "2026-08-24T18:00:00Z"
    )
    OffsetDateTime expiresAt
) {
}
