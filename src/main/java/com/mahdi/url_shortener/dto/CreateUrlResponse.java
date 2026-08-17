package com.mahdi.url_shortener.dto;

import java.time.OffsetDateTime;

public record CreateUrlResponse(
    String shortCode,
    String shortUrl,
    OffsetDateTime expiresAt
) {
    
}
