package com.mahdi.url_shortener.service;

import com.mahdi.url_shortener.entity.Url;
import com.mahdi.url_shortener.exception.UrlExpiredException;
import com.mahdi.url_shortener.exception.UrlNotFoundException;
import com.mahdi.url_shortener.repository.UrlRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class UrlServiceTest {
    
    @Mock
    private UrlRepository urlRepository;

    @Mock
    private RedisUrlCache redisUrlCache;

    @InjectMocks
    private UrlService urlService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    @Test
    void shouldReturnUrlFromRedisWhenCacheHit() {

        String shortCode = "abc1234";
    String originalUrl = "https://google.com";

    when(redisUrlCache.get(shortCode))
            .thenReturn(originalUrl);

    String result = urlService.getOriginalUrl(shortCode);

    assertEquals(originalUrl, result);

    verify(redisUrlCache).get(shortCode);
    verify(urlRepository).incrementClickCount(shortCode);

    }

    @Test
    void shouldLoadUrlFromDatabaseWhenCacheMiss() {

    String shortCode = "abc1234";
    String originalUrl = "https://google.com";

    Url url = new Url();
    url.setShortCode(shortCode);
    url.setOriginalUrl(originalUrl);
    url.setCreatedAt(OffsetDateTime.now());

    when(redisUrlCache.get(shortCode))
            .thenReturn(null);

    when(urlRepository.findByShortCode(shortCode))
            .thenReturn(Optional.of(url));

    String result = urlService.getOriginalUrl(shortCode);

    assertEquals(originalUrl, result);

    verify(redisUrlCache).save(
    shortCode,
    originalUrl,
    Duration.ofHours(1)
);
}

        @Test
        void shouldThrowExceptionWhenUrlDoesNotExist() {

    String shortCode = "abc1234";

    when(redisUrlCache.get(shortCode))
            .thenReturn(null);

    when(urlRepository.findByShortCode(shortCode))
            .thenReturn(Optional.empty());

    assertThrows(
            UrlNotFoundException.class,
            () -> urlService.getOriginalUrl(shortCode)
    );

    verify(redisUrlCache).get(shortCode);
    verify(urlRepository).findByShortCode(shortCode);
    verify(redisUrlCache, never())
        .save(anyString(), anyString(), any(Duration.class));
}

        @Test
        void shouldThrowExceptionWhenUrlIsExpired() {

    String shortCode = "expired1";
    String originalUrl = "https://example.com";

    Url url = new Url();
    url.setShortCode(shortCode);
    url.setOriginalUrl(originalUrl);
    url.setCreatedAt(OffsetDateTime.now().minusDays(2));
    url.setExpiresAt(OffsetDateTime.now().minusHours(1));

    when(redisUrlCache.get(shortCode))
            .thenReturn(null);

    when(urlRepository.findByShortCode(shortCode))
            .thenReturn(Optional.of(url));

    assertThrows(
            UrlExpiredException.class,
            () -> urlService.getOriginalUrl(shortCode)
    );

    verify(redisUrlCache).get(shortCode);
    verify(urlRepository).findByShortCode(shortCode);

    verify(redisUrlCache, never())
            .save(
                anyString(),
                anyString(),
                any(Duration.class)
            );
}

}
