package com.mahdi.url_shortener.controller;

import com.mahdi.url_shortener.dto.CreateUrlRequest;
import com.mahdi.url_shortener.dto.CreateUrlResponse;
import com.mahdi.url_shortener.dto.UrlStatsResponse;
import com.mahdi.url_shortener.service.UrlService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.OffsetDateTime;


@WebMvcTest(UrlController.class)
class UrlControllerTest {


    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private UrlService urlService;



    @Test
    void shouldCreateShortUrlSuccessfully() throws Exception {


        when(urlService.createShortUrl(any(CreateUrlRequest.class)))
                .thenReturn(
                        new CreateUrlResponse(
                                "abc1234",
                                "http://localhost:8080/abc1234",
                                null
                        )
                );


        mockMvc.perform(
                post("/api/v1/urls")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                    "url": "https://google.com"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.shortCode").value("abc1234"))
        .andExpect(jsonPath("$.shortUrl").value(
    "http://localhost:8080/abc1234"
        ))
        .andExpect(jsonPath("$.expiresAt").doesNotExist());
    }

    @Test
    void shouldReturn400WhenUrlIsInvalid() throws Exception {


    mockMvc.perform(
            post("/api/v1/urls")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "url": ""
                            }
                            """)
    )
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.status").value(400))
    .andExpect(jsonPath("$.errorCode")
            .value("VALIDATION_ERROR"));
    }

    @Test
        void shouldReturn400WhenJsonIsMalformed() throws Exception {

    mockMvc.perform(
            post("/api/v1/urls")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "url": "https://google.com"
                            """)
    )
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.status").value(400))
    .andExpect(jsonPath("$.errorCode")
            .value("INVALID_REQUEST"));
}

        @Test
        void shouldReturnUrlStats() throws Exception {

    OffsetDateTime createdAt =
        OffsetDateTime.parse("2026-08-17T10:00:00Z");

    UrlStatsResponse response =
        new UrlStatsResponse(
            "abc123",
            "https://example.com",
            42,
            createdAt,
            null
        );

    when(urlService.getUrlStats("abc123"))
        .thenReturn(response);

    mockMvc.perform(
        get("/api/v1/urls/abc123/stats")
    )
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.shortCode").value("abc123"))
    .andExpect(jsonPath("$.originalUrl").value(
        "https://example.com"
    ))
    .andExpect(jsonPath("$.clickCount").value(42))
    .andExpect(jsonPath("$.createdAt").value(
        "2026-08-17T10:00:00Z"
    ))
    .andExpect(jsonPath("$.expiresAt").doesNotExist());

    verify(urlService).getUrlStats("abc123");
        }

        @Test
    void shouldReturnExpirationTimeWhenCreatingShortUrl()
    throws Exception {

    OffsetDateTime expiresAt =
        OffsetDateTime.parse("2030-01-01T10:00:00Z");

    CreateUrlResponse response =
        new CreateUrlResponse(
            "abc1234",
            "http://localhost:8080/abc1234",
            expiresAt
        );

    when(urlService.createShortUrl(any(CreateUrlRequest.class)))
        .thenReturn(response);

    mockMvc.perform(
        post("/api/v1/urls")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "url": "https://example.com",
                    "expiresAt": "2030-01-01T10:00:00Z"
                }
                """)
    )
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.shortCode").value("abc1234"))
    .andExpect(jsonPath("$.shortUrl").value(
        "http://localhost:8080/abc1234"
    ))
    .andExpect(jsonPath("$.expiresAt").value(
        "2030-01-01T10:00:00Z"
    ));

    verify(urlService).createShortUrl(any(CreateUrlRequest.class));
    }
    }