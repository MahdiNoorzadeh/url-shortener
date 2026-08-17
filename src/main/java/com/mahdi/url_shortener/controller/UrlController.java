package com.mahdi.url_shortener.controller;

import com.mahdi.url_shortener.dto.CreateUrlRequest;
import com.mahdi.url_shortener.dto.CreateUrlResponse;
import com.mahdi.url_shortener.dto.UrlStatsResponse;
import com.mahdi.url_shortener.service.UrlService;
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
