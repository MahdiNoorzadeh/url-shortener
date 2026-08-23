package com.mahdi.url_shortener.controller;

import com.mahdi.url_shortener.service.UrlService;
import com.mahdi.url_shortener.exception.ErrorResponse;

import lombok.RequiredArgsConstructor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {
    
    private final UrlService urlService;
    

    @Operation(
    summary = "Redirect to original URL",
    description = "Redirects the client to the original URL associated with the short code."
    )
    @ApiResponses({
    @ApiResponse(
        responseCode = "302",
        description = "Redirect successful"
    ),
    @ApiResponse(
        responseCode = "404",
        description = "Short URL not found",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
        )
    ),
    @ApiResponse(
        responseCode = "410",
        description = "Short URL has expired",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ErrorResponse.class)
        )
    )
    })
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
        @Parameter(
        description = "The short code used to identify the original URL",
        example = "74bfddf"
        )
        @PathVariable String shortCode
    ) {
        String originalUrl =
            urlService.getOriginalUrl(shortCode);

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(originalUrl))
            .build();
    }

}
