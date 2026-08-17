package com.mahdi.url_shortener.dto;
import com.mahdi.url_shortener.validation.ValidUrl;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;

public record CreateUrlRequest(
        @NotBlank(message = "URL cannot be blank")
    @ValidUrl
    String url,

    OffsetDateTime expiresAt
) {
}
