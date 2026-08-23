package com.mahdi.url_shortener.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateUrlResponse(
    @Schema(
        description = "The generated short code",
        example = "74bfddf"
    )
    String shortCode,
    @Schema(
        description = "The complete shortened URL",
        example = "http://localhost:8080/74bfddf"
    )
    String shortUrl,
    @Schema(
        description = "Expiration date and time of the shortened URL",
        example = "2026-08-24T18:00:00Z"
    )
    OffsetDateTime expiresAt
) {
    
}
