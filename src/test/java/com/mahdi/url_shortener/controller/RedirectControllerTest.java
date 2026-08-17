package com.mahdi.url_shortener.controller;

import com.mahdi.url_shortener.service.UrlService;
import com.mahdi.url_shortener.exception.UrlNotFoundException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RedirectController.class)
public class RedirectControllerTest {
    
      @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;


    @Test
    void shouldRedirectToOriginalUrl() throws Exception {

        when(urlService.getOriginalUrl("abc1234"))
                .thenReturn("https://google.com");

        mockMvc.perform(
                get("/abc1234")
        )
        .andExpect(status().isFound())
        .andExpect(header().string(
                "Location",
                "https://google.com"
        ));
    }

    @Test
    void shouldReturn404WhenShortCodeDoesNotExist() throws Exception {

    when(urlService.getOriginalUrl("invalid1"))
            .thenThrow(new UrlNotFoundException("invalid1"));

    mockMvc.perform(
            get("/invalid1")
    )
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.status").value(404))
    .andExpect(jsonPath("$.errorCode")
            .value("URL_NOT_FOUND"));
}

}
