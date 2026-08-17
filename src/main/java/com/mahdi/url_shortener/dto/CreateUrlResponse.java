package com.mahdi.url_shortener.dto;

public record CreateUrlResponse(
    String shortCode,
    String shortUrl
) {
    
}
