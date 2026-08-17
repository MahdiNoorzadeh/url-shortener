package com.mahdi.url_shortener.repository;

import com.mahdi.url_shortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Transactional
    @Modifying
    @Query("""
    UPDATE Url u
    SET u.clickCount = u.clickCount + 1
    WHERE u.shortCode = :shortCode
        """)
    int incrementClickCount(
    @Param("shortCode") String shortCode
);
    
}
