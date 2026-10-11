package com.deb.service;

import com.deb.cache.TwoTierCacheService;
import com.deb.dto.CreateLinkRequest;
import com.deb.dto.LinkResponse;
import com.deb.entity.ShortLink;
import com.deb.exception.AliasGenerationException;
import com.deb.exception.LinkNotFoundException;
import com.deb.exception.SlugAlreadyExistsException;
import com.deb.generator.KeyGenerator;
import com.deb.repository.ShortLinkRepository;
import com.deb.util.UrlNormalizer;
import com.deb.validator.SlugValidator;
import com.deb.validator.UrlSecurityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShortLinkService {

    private static final Logger log = LoggerFactory.getLogger(ShortLinkService.class);
    private static final int MAX_KEYGEN_ATTEMPTS = 5;

    private final ShortLinkRepository shortLinkRepository;
    private final KeyGenerator keyGenerator;
    private final UrlSecurityValidator urlSecurityValidator;
    private final SlugValidator slugValidator;
    private final TwoTierCacheService twoTierCacheService;
    private final UrlNormalizer urlNormalizer;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Transactional
    public LinkResponse createLink(CreateLinkRequest request) {
        String normalizedUrl = normalizeAndValidateUrl(request.getOriginalUrl());

        // Check if normalized URL already shortened
        Optional<ShortLink> existingOpt = shortLinkRepository.findByOriginalUrl(normalizedUrl);
        if (existingOpt.isPresent() && !StringUtils.hasText(request.getCustomSlug())) {
            ShortLink existing = existingOpt.get();
            log.info("URL '{}' already shortened as '{}'. Returning existing link.", normalizedUrl, existing.getShortCode());
            return mapToResponse(existing);
        }

        String shortCode;
        if (StringUtils.hasText(request.getCustomSlug())) {
            shortCode = request.getCustomSlug().trim();
            slugValidator.validateSlug(shortCode);
            if (shortLinkRepository.existsByShortCode(shortCode)) {
                throw new SlugAlreadyExistsException("Custom slug '" + shortCode + "' is already in use.");
            }
        } else {
            shortCode = generateUniqueKey();
        }

        LocalDateTime expiresAt = calculateExpiration(request);

        ShortLink newLink = ShortLink.builder()
                .shortCode(shortCode)
                .originalUrl(normalizedUrl)
                .expiresAt(expiresAt)
                .isActive(true)
                .version(0L)
                .build();

        ShortLink saved = shortLinkRepository.save(newLink);
        log.info("Created short link: code='{}', url='{}', expiresAt={}", saved.getShortCode(), saved.getOriginalUrl(), saved.getExpiresAt());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public LinkResponse getLink(String shortCode) {
        log.debug("Fetching short link from repository: '{}'", shortCode);
        ShortLink link = shortLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException("Short link '" + shortCode + "' not found"));
        log.info("Found short link: code='{}', destination='{}'", shortCode, link.getOriginalUrl());
        return mapToResponse(link);
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> listLinks() {
        log.debug("Fetching all short links from repository");
        List<ShortLink> all = shortLinkRepository.findAll();
        log.info("Retrieved {} short links from repository", all.size());
        return all.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public LinkResponse deactivateLink(String shortCode) {
        ShortLink link = shortLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException("Short link '" + shortCode + "' not found"));

        link.setIsActive(false);
        ShortLink updated = shortLinkRepository.save(link);

        // Evict from caches and broadcast via Redis Pub/Sub
        twoTierCacheService.evict(shortCode);

        log.info("Deactivated short link: '{}'", shortCode);
        return mapToResponse(updated);
    }

    @Transactional
    public LinkResponse updateDestinationUrl(String shortCode, String newUrl) {
        ShortLink link = shortLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException("Short link '" + shortCode + "' not found"));

        String normalizedUrl = normalizeAndValidateUrl(newUrl);
        link.setOriginalUrl(normalizedUrl);
        ShortLink updated = shortLinkRepository.save(link);

        // Evict stale cache entries across all nodes
        twoTierCacheService.evict(shortCode);

        log.info("Updated destination for short link '{}' to '{}'", shortCode, normalizedUrl);
        return mapToResponse(updated);
    }

    private String generateUniqueKey() {
        int attempts = 0;
        while (attempts < MAX_KEYGEN_ATTEMPTS) {
            String candidate = keyGenerator.generateKey();
            if (!shortLinkRepository.existsByShortCode(candidate)) {
                return candidate;
            }
            attempts++;
            log.warn("Key collision detected for '{}' on attempt {}. Retrying...", candidate, attempts);
        }
        throw new AliasGenerationException("Could not generate a unique key after " + MAX_KEYGEN_ATTEMPTS + " attempts.");
    }

    private String normalizeAndValidateUrl(String rawUrl) {
        String normalized = urlNormalizer.normalize(rawUrl);
        urlSecurityValidator.validateUrl(normalized);
        return normalized;
    }

    private LocalDateTime calculateExpiration(CreateLinkRequest request) {
        if (request.getExpiresAt() != null) {
            return request.getExpiresAt();
        }
        if (request.getExpiresInDays() != null && request.getExpiresInDays() > 0) {
            return LocalDateTime.now().plusDays(request.getExpiresInDays());
        }
        return null;
    }

    public LinkResponse mapToResponse(ShortLink link) {
        String fullShortUrl = appBaseUrl.replaceAll("/+$", "") + "/r/" + link.getShortCode();
        return LinkResponse.builder()
                .id(link.getId())
                .shortCode(link.getShortCode())
                .originalUrl(link.getOriginalUrl())
                .shortUrl(fullShortUrl)
                .createdAt(link.getCreatedAt())
                .expiresAt(link.getExpiresAt())
                .isActive(link.getIsActive())
                .build();
    }
}
