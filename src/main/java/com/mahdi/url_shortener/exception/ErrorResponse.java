package com.mahdi.url_shortener.exception;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
    @Schema(
        description = "HTTP status code",
        example = "404"
    )
    int status,
    @Schema(
        description = "Application-specific error code",
        example = "URL_NOT_FOUND"
    )
    String errorCode,
    @Schema(
        description = "Human-readable error message",
        example = "Short URL not found"
    )
    String message,
        @Schema(
        description = "Time when the error occurred",
        example = "2026-08-23T22:30:00"
    )
    LocalDateTime timestamp
) {
    
}
