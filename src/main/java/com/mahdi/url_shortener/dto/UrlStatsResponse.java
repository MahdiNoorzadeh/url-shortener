package com.mahdi.url_shortener.dto;

import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record UrlStatsResponse (
        @Schema(
        description = "The generated short code",
        example = "74bfddf"
    )
    String shortCode,
        @Schema(
        description = "The original URL associated with the short code",
        example = "https://example.com"
    )
    String originalUrl,
        @Schema(
        description = "Number of times the shortened URL has been accessed",
        example = "42"
    )
    long clickCount,
        @Schema(
        description = "Date and time when the shortened URL was created",
        example = "2026-08-23T18:30:00Z"
    )
    OffsetDateTime createdAt,
        @Schema(
        description = "Expiration date and time of the shortened URL",
        example = "2026-08-24T18:30:00Z"
    )
    OffsetDateTime expiresAt
)
{}
