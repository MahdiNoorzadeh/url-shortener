package com.mahdi.url_shortener.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
    int status,
    String errorCode,
    String message,
    LocalDateTime timestamp
) {
    
}
