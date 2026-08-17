package com.mahdi.url_shortener.controller;

import com.mahdi.url_shortener.dto.CreateUrlRequest;
import com.mahdi.url_shortener.dto.CreateUrlResponse;
import com.mahdi.url_shortener.service.UrlService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


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
                                "http://localhost:8080/abc1234"
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
        .andExpect(jsonPath("$.shortUrl")
                .value("http://localhost:8080/abc1234"));
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
}