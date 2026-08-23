package com.mahdi.url_shortener.controller;

import com.mahdi.url_shortener.dto.CreateUrlRequest;
import com.mahdi.url_shortener.dto.CreateUrlResponse;
import com.mahdi.url_shortener.dto.UrlStatsResponse;
import com.mahdi.url_shortener.service.UrlService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.validation.Valid;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {
    
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @Operation(
        summary = "Create a short URL",
        description = "Creates a shortened URL from the provided original URL."
    )
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Short URL created successfully"
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid URL"
        )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUrlResponse createUrl(
        @Valid @RequestBody CreateUrlRequest request
    ) {
        return urlService.createShortUrl(request);
    }

    @GetMapping("/{shortCode}/stats")
    public UrlStatsResponse getUrlStats(
    @PathVariable String shortCode
    ) {
    return urlService.getUrlStats(shortCode);
    }
}
