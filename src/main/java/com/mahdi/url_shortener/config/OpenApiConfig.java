package com.mahdi.url_shortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI urlShortenerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .description("REST API for creating, managing, and redirecting shortened URLs.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Mahdi Noorzadeh")
                                .email("mahdinoorzadeh1@gmail.com")
                                .url(null)
                        )
                );
    }
    
}
