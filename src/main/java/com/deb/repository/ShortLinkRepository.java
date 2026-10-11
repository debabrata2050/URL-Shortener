package com.deb.repository;

import com.deb.entity.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {
    Optional<ShortLink> findByShortCode(String shortCode);
    Optional<ShortLink> findByOriginalUrl(String originalUrl);
    boolean existsByShortCode(String shortCode);
}
