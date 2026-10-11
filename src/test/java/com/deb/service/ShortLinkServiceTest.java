package com.deb.service;

import com.deb.cache.TwoTierCacheService;
import com.deb.dto.CreateLinkRequest;
import com.deb.dto.LinkResponse;
import com.deb.entity.ShortLink;
import com.deb.exception.LinkNotFoundException;
import com.deb.exception.SlugAlreadyExistsException;
import com.deb.generator.KeyGenerator;
import com.deb.repository.ShortLinkRepository;
import com.deb.util.UrlNormalizer;
import com.deb.validator.SlugValidator;
import com.deb.validator.UrlSecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private KeyGenerator keyGenerator;

    @Mock
    private UrlSecurityValidator urlSecurityValidator;

    @Mock
    private SlugValidator slugValidator;

    @Mock
    private TwoTierCacheService twoTierCacheService;

    @Spy
    private UrlNormalizer urlNormalizer = new UrlNormalizer();

    @InjectMocks
    private ShortLinkService shortLinkService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shortLinkService, "appBaseUrl", "http://localhost:8080");
    }

    @Test
    @DisplayName("Create link with generated key succeeds")
    void testCreateLinkWithGeneratedKey() {
        CreateLinkRequest request = CreateLinkRequest.builder()
                .originalUrl("https://example.com/long-page")
                .build();

        when(shortLinkRepository.findByOriginalUrl("https://example.com/long-page")).thenReturn(Optional.empty());
        when(keyGenerator.generateKey()).thenReturn("14qV5Q");
        when(shortLinkRepository.existsByShortCode("14qV5Q")).thenReturn(false);

        ShortLink savedLink = ShortLink.builder()
                .id(1L)
                .shortCode("14qV5Q")
                .originalUrl("https://example.com/long-page")
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .version(0L)
                .build();

        when(shortLinkRepository.save(any(ShortLink.class))).thenReturn(savedLink);

        LinkResponse response = shortLinkService.createLink(request);

        assertNotNull(response);
        assertEquals("14qV5Q", response.getShortCode());
        assertEquals("http://localhost:8080/r/14qV5Q", response.getShortUrl());
        verify(urlSecurityValidator).validateUrl("https://example.com/long-page");
        verify(shortLinkRepository).save(any(ShortLink.class));
    }

    @Test
    @DisplayName("Create link with custom slug succeeds")
    void testCreateLinkWithCustomSlug() {
        CreateLinkRequest request = CreateLinkRequest.builder()
                .originalUrl("https://example.com")
                .customSlug("my-custom-slug")
                .build();

        when(shortLinkRepository.findByOriginalUrl("https://example.com")).thenReturn(Optional.empty());
        when(shortLinkRepository.existsByShortCode("my-custom-slug")).thenReturn(false);

        ShortLink saved = ShortLink.builder()
                .id(2L)
                .shortCode("my-custom-slug")
                .originalUrl("https://example.com")
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .version(0L)
                .build();

        when(shortLinkRepository.save(any(ShortLink.class))).thenReturn(saved);

        LinkResponse response = shortLinkService.createLink(request);

        assertEquals("my-custom-slug", response.getShortCode());
        verify(slugValidator).validateSlug("my-custom-slug");
        verify(keyGenerator, never()).generateKey();
    }

    @Test
    @DisplayName("Create link with duplicate custom slug throws SlugAlreadyExistsException")
    void testCreateLinkWithDuplicateCustomSlugThrows() {
        CreateLinkRequest request = CreateLinkRequest.builder()
                .originalUrl("https://example.com")
                .customSlug("already-taken")
                .build();

        when(shortLinkRepository.findByOriginalUrl("https://example.com")).thenReturn(Optional.empty());
        when(shortLinkRepository.existsByShortCode("already-taken")).thenReturn(true);

        assertThrows(SlugAlreadyExistsException.class, () -> shortLinkService.createLink(request));
        verify(shortLinkRepository, never()).save(any());
    }

    @Test
    @DisplayName("Collision retry logic invokes key generator multiple times")
    void testCollisionRetry() {
        CreateLinkRequest request = CreateLinkRequest.builder()
                .originalUrl("https://example.com/retry-test")
                .build();

        when(shortLinkRepository.findByOriginalUrl("https://example.com/retry-test")).thenReturn(Optional.empty());
        // First key collides, second key succeeds
        when(keyGenerator.generateKey()).thenReturn("collisionKey", "uniqueKey");
        when(shortLinkRepository.existsByShortCode("collisionKey")).thenReturn(true);
        when(shortLinkRepository.existsByShortCode("uniqueKey")).thenReturn(false);

        ShortLink saved = ShortLink.builder()
                .id(3L)
                .shortCode("uniqueKey")
                .originalUrl("https://example.com/retry-test")
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(shortLinkRepository.save(any(ShortLink.class))).thenReturn(saved);

        LinkResponse response = shortLinkService.createLink(request);

        assertEquals("uniqueKey", response.getShortCode());
        verify(keyGenerator, times(2)).generateKey();
    }

    @Test
    @DisplayName("Deactivate link updates database and evicts caches")
    void testDeactivateLink() {
        ShortLink link = ShortLink.builder()
                .id(1L)
                .shortCode("activeLink")
                .originalUrl("https://example.com")
                .isActive(true)
                .build();

        when(shortLinkRepository.findByShortCode("activeLink")).thenReturn(Optional.of(link));
        when(shortLinkRepository.save(any(ShortLink.class))).thenReturn(link);

        LinkResponse response = shortLinkService.deactivateLink("activeLink");

        assertFalse(response.getIsActive());
        verify(twoTierCacheService).evict("activeLink");
    }

    @Test
    @DisplayName("Get nonexistent link throws LinkNotFoundException")
    void testGetNonexistentLinkThrows() {
        when(shortLinkRepository.findByShortCode("nonexistent")).thenReturn(Optional.empty());

        assertThrows(LinkNotFoundException.class, () -> shortLinkService.getLink("nonexistent"));
    }
}
