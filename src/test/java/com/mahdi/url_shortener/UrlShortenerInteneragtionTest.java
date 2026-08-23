package com.mahdi.url_shortener;

import com.mahdi.url_shortener.entity.Url;
import com.mahdi.url_shortener.repository.UrlRepository;
import com.mahdi.url_shortener.service.RedisUrlCache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
public class UrlShortenerInteneragtionTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

     @Autowired
    private MockMvc mockMvc;

      @Autowired
    private UrlRepository urlRepository;
    
    @Autowired
    private RedisUrlCache redisUrlCache;

    @Container
    static PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:17")
                .withDatabaseName("url_shortener")
                .withUsername("url_shortener")
                .withPassword("url_shortener");

    @Container
    static GenericContainer redis =
        new GenericContainer("redis:7-alpine")
                .withExposedPorts(6379);
                
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    registry.add("spring.data.redis.host", redis::getHost);
    registry.add(
            "spring.data.redis.port",
            () -> redis.getMappedPort(6379)
    );
}


    @Test
    void applicationContextShouldLoad() {

        assertTrue(postgres.isRunning());
        assertTrue(redis.isRunning());
    }


    @Test
    void shouldSaveAndLoadUrlFromPostgreSQL() {

    Url url = new Url();

    url.setShortCode("abc1234");
    url.setOriginalUrl("https://google.com");
    url.setCreatedAt(OffsetDateTime.now());

    urlRepository.save(url);

    var result = urlRepository.findByShortCode("abc1234");

    assertTrue(result.isPresent());
    assertEquals(
            "https://google.com",
            result.get().getOriginalUrl()
    );
}

    @Test
    void shouldCreateShortUrlThroughFullApplicationFlow() throws Exception {

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
    .andExpect(jsonPath("$.shortCode").isNotEmpty())
    .andExpect(jsonPath("$.shortUrl").isNotEmpty());
}

    @BeforeEach
    void clearRedis() {
    redisTemplate.getConnectionFactory()
            .getConnection()
            .serverCommands()
            .flushDb();
}

    @Test
    void shouldLoadUrlFromPostgreSQLAndCacheItInRedis() throws Exception {

    Url url = new Url();
    url.setShortCode("cache123");
    url.setOriginalUrl("https://google.com");
    url.setCreatedAt(OffsetDateTime.now());

    urlRepository.save(url);

    mockMvc.perform(
            get("/cache123")
    )
    .andExpect(status().isFound())
    .andExpect(header().string(
            "Location",
            "https://google.com"
    ));

    String cachedUrl =
            redisTemplate.opsForValue().get("url:cache123");

    assertEquals(
            "https://google.com",
            cachedUrl
    );
}
        @Test
    void shouldReturnUrlFromRedisCacheWithoutPostgreSQL() throws Exception {

    redisTemplate.opsForValue().set(
            "url:hit123",
            "https://google.com"
    );

    mockMvc.perform(
            get("/hit123")
    )
    .andExpect(status().isFound())
    .andExpect(header().string(
            "Location",
            "https://google.com"
    ));
}

    @Test
    void shouldSaveUrlToRedisWithOneHourTtl() {

    redisUrlCache.save(
            "ttl123",
            "https://google.com",
            Duration.ofHours(1)
    );

    String cachedUrl =
            redisTemplate.opsForValue().get("url:ttl123");

    Long ttl =
            redisTemplate.getExpire("url:ttl123");

    assertEquals(
            "https://google.com",
            cachedUrl
    );

    assertTrue(ttl > 0);
    assertTrue(ttl <= 3600);
}

    @Test
    void shouldCreateUrlThenRedirectAndCacheIt() throws Exception {

    String response = mockMvc.perform(
            post("/api/v1/urls")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "url": "https://example.com"
                            }
                            """)
    )
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.shortCode").isNotEmpty())
    .andReturn()
    .getResponse()
    .getContentAsString();

    String shortCode = new tools.jackson.databind.ObjectMapper()
            .readTree(response)
            .get("shortCode")
            .asString();

    mockMvc.perform(
            get("/" + shortCode)
    )
    .andExpect(status().isFound())
    .andExpect(header().string(
            "Location",
            "https://example.com"
    ));

    String cachedUrl = redisTemplate.opsForValue().get(
            "url:" + shortCode
    );

    assertEquals(
            "https://example.com",
            cachedUrl
    );

    mockMvc.perform(
            get("/" + shortCode)
    )
    .andExpect(status().isFound())
    .andExpect(header().string(
            "Location",
            "https://example.com"
    ));
}

    @Test
    void shouldReturn404WhenShortCodeDoesNotExist() throws Exception {

    mockMvc.perform(
            get("/doesnotexist")
    )
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.status").value(404))
    .andExpect(jsonPath("$.errorCode").value("URL_NOT_FOUND"))
    .andExpect(jsonPath("$.message").value(
            "Short URL not found: doesnotexist"
    ));
}

    @Test
    void shouldReturn400WhenUrlIsBlank() throws Exception {

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
    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
}

    @Test
    void shouldReturn400WhenUrlFormatIsInvalid() throws Exception {

    mockMvc.perform(
            post("/api/v1/urls")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "url": "not-a-valid-url"
                            }
                            """)
    )
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.status").value(400))
    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
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
    .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
    .andExpect(jsonPath("$.message").value(
            "Malformed JSON request"
    ));
}

    @Test
    void shouldSaveExpirationTimeToPostgreSQL() throws Exception {

        OffsetDateTime expiresAt =
        OffsetDateTime.now()
                .plusDays(1);

        expiresAt = expiresAt.withNano(
        (expiresAt.getNano() / 1000) * 1000
        );
    String response = mockMvc.perform(
            post("/api/v1/urls")
                    .contentType(APPLICATION_JSON)
                    .content("""
                            {
                                "url": "https://example.com",
                                "expiresAt": "%s"
                            }
                            """.formatted(expiresAt))
    )
    .andExpect(status().isCreated())
    .andReturn()
    .getResponse()
    .getContentAsString();

    String shortCode = new tools.jackson.databind.ObjectMapper()
            .readTree(response)
            .get("shortCode")
            .asString();

    Url url = urlRepository.findByShortCode(shortCode)
            .orElseThrow();


        System.out.println("Expected expiresAt: " + expiresAt);
        System.out.println("Actual expiresAt:   " + url.getExpiresAt());
        assertTrue(
        expiresAt.isEqual(url.getExpiresAt())
        );
}

        @Test
        void shouldReturn410WhenUrlIsExpired() throws Exception {

    Url url = new Url();

    url.setShortCode("expired123");
    url.setOriginalUrl("https://example.com");
    url.setCreatedAt(OffsetDateTime.now().minusDays(2));
    url.setExpiresAt(OffsetDateTime.now().minusHours(1));

    urlRepository.save(url);

    mockMvc.perform(
            get("/expired123")
    )
    .andExpect(status().isGone())
    .andExpect(jsonPath("$.status").value(410))
    .andExpect(jsonPath("$.errorCode").value("URL_EXPIRED"))
    .andExpect(jsonPath("$.message").value(
            "Short URL expired: expired123"
    ));

    String cachedUrl =
            redisTemplate.opsForValue().get("url:expired123");

    assertEquals(null, cachedUrl);
}

        @Test
        void shouldRedirectWhenUrlHasNoExpiration() throws Exception {

    Url url = new Url();

    url.setShortCode("noexpiry");
    url.setOriginalUrl("https://example.com");
    url.setCreatedAt(OffsetDateTime.now());
    url.setExpiresAt(null);

    urlRepository.save(url);

    mockMvc.perform(
            get("/noexpiry")
    )
    .andExpect(status().isFound())
    .andExpect(header().string(
            "Location",
            "https://example.com"
    ));

    String cachedUrl =
            redisTemplate.opsForValue().get("url:noexpiry");

    assertEquals(
            "https://example.com",
            cachedUrl
    );
}


        @Test
        void shouldSetRedisTtlBasedOnExpirationTime() throws Exception {

    OffsetDateTime expiresAt =
        OffsetDateTime.now().plusMinutes(10);

    Url url = new Url();

    url.setShortCode("expirettl");
    url.setOriginalUrl("https://example.com");
    url.setCreatedAt(OffsetDateTime.now());
    url.setExpiresAt(expiresAt);

    urlRepository.save(url);

    mockMvc.perform(
        get("/expirettl")
    )
    .andExpect(status().isFound())
    .andExpect(header().string(
        "Location",
        "https://example.com"
    ));

    Long ttl =
        redisTemplate.getExpire("url:expirettl");

    assertTrue(ttl > 0);
    assertTrue(ttl <= 600);
    assertTrue(ttl >= 590);
}

        @Test
        void shouldExpireRedisCacheWhenUrlExpires() throws Exception {

    OffsetDateTime expiresAt =
        OffsetDateTime.now().plusSeconds(2);

    Url url = new Url();

    url.setShortCode("expirecache");
    url.setOriginalUrl("https://example.com");
    url.setCreatedAt(OffsetDateTime.now());
    url.setExpiresAt(expiresAt);

    urlRepository.save(url);

    // First request: URL is still valid
    mockMvc.perform(
        get("/expirecache")
    )
    .andExpect(status().isFound())
    .andExpect(header().string(
        "Location",
        "https://example.com"
    ));

    String cachedUrl =
        redisTemplate.opsForValue().get("url:expirecache");

    assertEquals(
        "https://example.com",
        cachedUrl
    );

    // Wait until Redis TTL expires
    Thread.sleep(2500);

    // Second request: URL should now be expired
    mockMvc.perform(
        get("/expirecache")
    )
    .andExpect(status().isGone())
    .andExpect(jsonPath("$.status").value(410))
    .andExpect(jsonPath("$.errorCode").value("URL_EXPIRED"));
}

        @Test
        void shouldIncrementClickCountOnEachSuccessfulRedirect() throws Exception {

    Url url = new Url();

    url.setShortCode("clicktest");
    url.setOriginalUrl("https://example.com");
    url.setCreatedAt(OffsetDateTime.now());
    url.setClickCount(0);

    urlRepository.save(url);

    mockMvc.perform(
        get("/clicktest")
    )
    .andExpect(status().isFound())
    .andExpect(header().string(
        "Location",
        "https://example.com"
    ));

    Url afterFirstClick =
        urlRepository.findByShortCode("clicktest")
            .orElseThrow();

    assertEquals(
        1,
        afterFirstClick.getClickCount()
    );

    mockMvc.perform(
        get("/clicktest")
    )
    .andExpect(status().isFound())
    .andExpect(header().string(
        "Location",
        "https://example.com"
    ));

    Url afterSecondClick =
        urlRepository.findByShortCode("clicktest")
            .orElseThrow();

    assertEquals(
        2,
        afterSecondClick.getClickCount()
    );
}

        @Test
        void shouldNotIncrementClickCountWhenUrlDoesNotExist() throws Exception {

    mockMvc.perform(
        get("/doesnotexist")
    )
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.status").value(404))
    .andExpect(jsonPath("$.errorCode").value("URL_NOT_FOUND"));
}
        @Test
        void shouldNotIncrementClickCountWhenUrlIsExpired() throws Exception {

    Url url = new Url();

    url.setShortCode("expiredclick");
    url.setOriginalUrl("https://example.com");
    url.setCreatedAt(OffsetDateTime.now().minusHours(2));
    url.setExpiresAt(OffsetDateTime.now().minusHours(1));
    url.setClickCount(0);

    urlRepository.save(url);

    mockMvc.perform(
        get("/expiredclick")
    )
    .andExpect(status().isGone())
    .andExpect(jsonPath("$.status").value(410))
    .andExpect(jsonPath("$.errorCode").value("URL_EXPIRED"));

    Url afterRequest =
        urlRepository.findByShortCode("expiredclick")
            .orElseThrow();

    assertEquals(
        0,
        afterRequest.getClickCount()
    );
}

    @Test
    void shouldReturnUrlStatsFromPostgreSQL() throws Exception {

    Url url = new Url();

    url.setShortCode("statstest");
    url.setOriginalUrl("https://example.com");
    url.setCreatedAt(
        OffsetDateTime.parse("2026-08-17T10:00:00Z")
    );
    url.setExpiresAt(null);
    url.setClickCount(42);

    urlRepository.save(url);

    mockMvc.perform(
        get("/api/v1/urls/statstest/stats")
    )
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.shortCode").value("statstest"))
    .andExpect(jsonPath("$.originalUrl").value(
        "https://example.com"
    ))
    .andExpect(jsonPath("$.clickCount").value(42))
    .andExpect(jsonPath("$.createdAt").value(
        "2026-08-17T10:00:00Z"
    ))
    .andExpect(jsonPath("$.expiresAt").doesNotExist());
    }

    @Test
    void shouldReturn404WhenRequestingStatsForUnknownUrl()
    throws Exception {

    mockMvc.perform(
        get("/api/v1/urls/doesnotexist/stats")
    )
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.status").value(404))
    .andExpect(jsonPath("$.errorCode").value(
        "URL_NOT_FOUND"
    ))
    .andExpect(jsonPath("$.message").value(
        "Short URL not found: doesnotexist"
    ));
    }

    @Test
    void shouldReturnExpirationTimeInUrlStats() throws Exception {

    OffsetDateTime createdAt =
        OffsetDateTime.parse("2026-08-17T10:00:00Z");

    OffsetDateTime expiresAt =
        OffsetDateTime.parse("2026-08-18T10:00:00Z");

    Url url = new Url();

    url.setShortCode("expirestats");
    url.setOriginalUrl("https://example.com");
    url.setCreatedAt(createdAt);
    url.setExpiresAt(expiresAt);
    url.setClickCount(15);

    urlRepository.save(url);

    mockMvc.perform(
        get("/api/v1/urls/expirestats/stats")
    )
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.shortCode").value("expirestats"))
    .andExpect(jsonPath("$.originalUrl").value(
        "https://example.com"
    ))
    .andExpect(jsonPath("$.clickCount").value(15))
    .andExpect(jsonPath("$.createdAt").value(
        "2026-08-17T10:00:00Z"
    ))
    .andExpect(jsonPath("$.expiresAt").value(
        "2026-08-18T10:00:00Z"
    ));
    }

    @Test
    void shouldRejectExpiredTimeWhenCreatingShortUrl()
    throws Exception {

    String requestBody = """
        {
            "url": "https://example.com",
            "expiresAt": "2020-01-01T10:00:00Z"
        }
        """;

    mockMvc.perform(
        post("/api/v1/urls")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
    )
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.status").value(400))
    .andExpect(jsonPath("$.errorCode").value(
        "INVALID_EXPIRATION_TIME"
    ))
    .andExpect(jsonPath("$.message").value(
        "Expiration time must be in the future"
    ));
    }

    @Test
    void shouldCreateShortUrlWithFutureExpirationTime()
    throws Exception {

    String requestBody = """
        {
            "url": "https://example.com",
            "expiresAt": "2030-01-01T10:00:00Z"
        }
        """;

    mockMvc.perform(
        post("/api/v1/urls")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
    )
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.shortCode").exists())
    .andExpect(jsonPath("$.shortUrl").exists());
    }

    @Test
    void shouldCreateShortUrlWithoutExpirationTime()
    throws Exception {

    String requestBody = """
        {
            "url": "https://example.com"
        }
        """;

    mockMvc.perform(
        post("/api/v1/urls")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
    )
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.shortCode").exists())
    .andExpect(jsonPath("$.shortUrl").exists())
    .andExpect(jsonPath("$.expiresAt").doesNotExist());
    }
}
