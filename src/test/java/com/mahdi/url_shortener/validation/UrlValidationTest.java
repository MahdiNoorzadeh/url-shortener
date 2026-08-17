package com.mahdi.url_shortener.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UrlValidationTest {
    
    private final UrlValidator validator =
        new UrlValidator();

    @Test
    void shouldAcceptHttpsUrl() {

        assertTrue(
            validator.isValid(
                "https://example.com",
                null
            )
        );
    }

    @Test
    void shouldAcceptHttpUrl() {

        assertTrue(
            validator.isValid(
                "http://example.com",
                null
            )
        );
    }

    @Test
    void shouldAcceptUrlWithQueryParameters() {

        assertTrue(
            validator.isValid(
                "https://example.com/search?q=java",
                null
            )
        );
    }

    @Test
    void shouldAcceptUrlWithPort() {

        assertTrue(
            validator.isValid(
                "http://example.com:8080/api",
                null
            )
        );
    }

    @Test
    void shouldRejectUrlWithoutProtocol() {

        assertFalse(
            validator.isValid(
                "example.com",
                null
            )
        );
    }

    @Test
    void shouldRejectFtpUrl() {

        assertFalse(
            validator.isValid(
                "ftp://example.com",
                null
            )
        );
    }

    @Test
    void shouldRejectUrlWithoutHost() {

        assertFalse(
            validator.isValid(
                "https://",
                null
            )
        );
    }

    @Test
    void shouldAcceptNullValue() {

        assertTrue(
            validator.isValid(
                null,
                null
            )
        );
    }

    @Test
    void shouldAcceptBlankValue() {

        assertTrue(
            validator.isValid(
                "   ",
                null
            )
        );
    }
}
