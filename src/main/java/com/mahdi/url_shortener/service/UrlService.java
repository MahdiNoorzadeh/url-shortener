package com.mahdi.url_shortener.service;

import com.mahdi.url_shortener.dto.CreateUrlRequest;
import com.mahdi.url_shortener.dto.CreateUrlResponse;
import com.mahdi.url_shortener.entity.Url;
import com.mahdi.url_shortener.exception.UrlNotFoundException;
import com.mahdi.url_shortener.repository.UrlRepository;
import com.mahdi.url_shortener.exception.UrlExpiredException;
import com.mahdi.url_shortener.dto.UrlStatsResponse;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {
    
    private final UrlRepository urlRepository;
    private final RedisUrlCache redisUrlCache;

    public CreateUrlResponse createShortUrl(CreateUrlRequest request) {

        String shortCode = generateUniqueShortCode();

        Url url = new Url();
        url.setOriginalUrl(request.url());
        url.setShortCode(shortCode);
        url.setCreatedAt(OffsetDateTime.now());
        url.setExpiresAt(request.expiresAt());

        urlRepository.save(url);
        log.info("Short URL created successfully: shortCode={}", shortCode);

        return new CreateUrlResponse(
            shortCode,
            "http://localhost:8080/" + shortCode
        );
    }

    private String generateUniqueShortCode() {

        String shortCode;

        do {
            shortCode = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 7);

        } while (urlRepository.existsByShortCode(shortCode));

        return shortCode;
    }

    public String getOriginalUrl(String shortCode) {

       String cachedUrl = redisUrlCache.get(shortCode);

    if (cachedUrl != null) {
        log.debug("Redis cache HIT: shortCode={}", shortCode);

        urlRepository.incrementClickCount(shortCode);

        return cachedUrl;
    }

    log.debug("Redis cache MISS: shortCode={}", shortCode);

    Url url = urlRepository.findByShortCode(shortCode)
        .orElseThrow(() ->
            new UrlNotFoundException(shortCode)
        );

    OffsetDateTime now = OffsetDateTime.now();

    if (
        url.getExpiresAt() != null &&
        !url.getExpiresAt().isAfter(now)
    ) {
        throw new UrlExpiredException(shortCode);
    }

    log.debug("URL loaded from PostgreSQL: shortCode={}", shortCode);

    Duration ttl = Duration.ofHours(1);

    if (url.getExpiresAt() != null) {
        ttl = Duration.between(
            now,
            url.getExpiresAt()
        );
    }

    redisUrlCache.save(
        shortCode,
        url.getOriginalUrl(),
        ttl
    );

    urlRepository.incrementClickCount(shortCode);

    return url.getOriginalUrl();
    }

    public UrlStatsResponse getUrlStats(String shortCode) {

    Url url = urlRepository.findByShortCode(shortCode)
        .orElseThrow(() ->
            new UrlNotFoundException(shortCode)
        );

    return new UrlStatsResponse(
        url.getShortCode(),
        url.getOriginalUrl(),
        url.getClickCount(),
        url.getCreatedAt(),
        url.getExpiresAt()
    );
}
}
