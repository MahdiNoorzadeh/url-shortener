package com.mahdi.url_shortener.dto;

import java.time.OffsetDateTime;

public record UrlStatsResponse (
    String shortCode,
    String originalUrl,
    long clickCount,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt
)
{}
