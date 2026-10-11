package com.deb.cache;

import com.deb.cache.model.CachedLinkDto;
import com.deb.entity.ShortLink;
import com.deb.exception.LinkNotFoundException;
import com.deb.repository.ShortLinkRepository;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoTierCacheServiceTest {

    @Mock
    private Cache<String, CachedLinkDto> l1Cache;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @InjectMocks
    private TwoTierCacheService cacheService;

    @Test
    @DisplayName("L1 cache HIT returns immediately without querying Redis or MySQL")
    void testL1CacheHit() {
        CachedLinkDto cached = CachedLinkDto.builder()
                .id(1L)
                .shortCode("l1HitKey")
                .originalUrl("https://example.com")
                .isActive(true)
                .isSentinel(false)
                .build();

        when(l1Cache.getIfPresent("l1HitKey")).thenReturn(cached);

        CachedLinkDto result = cacheService.getResolvedLink("l1HitKey");

        assertNotNull(result);
        assertEquals("https://example.com", result.getOriginalUrl());
        verifyNoInteractions(redisTemplate);
        verifyNoInteractions(shortLinkRepository);
    }

    @Test
    @DisplayName("L1 cache MISS and L2 Redis HIT populates L1 and returns without querying database")
    void testL1MissL2Hit() {
        when(l1Cache.getIfPresent("l2HitKey")).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        CachedLinkDto redisLink = CachedLinkDto.builder()
                .id(2L)
                .shortCode("l2HitKey")
                .originalUrl("https://redis-found.com")
                .isActive(true)
                .isSentinel(false)
                .build();

        when(valueOperations.get("link:l2HitKey")).thenReturn(redisLink);

        CachedLinkDto result = cacheService.getResolvedLink("l2HitKey");

        assertNotNull(result);
        assertEquals("https://redis-found.com", result.getOriginalUrl());
        verify(l1Cache).put("l2HitKey", redisLink);
        verifyNoInteractions(shortLinkRepository);
    }

    @Test
    @DisplayName("L1 and L2 MISS queries database and backfills both caches")
    void testL1MissL2MissDatabaseHit() {
        when(l1Cache.getIfPresent("dbKey")).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("link:dbKey")).thenReturn(null);

        ShortLink dbLink = ShortLink.builder()
                .id(3L)
                .shortCode("dbKey")
                .originalUrl("https://from-database.com")
                .expiresAt(LocalDateTime.now().plusHours(5))
                .isActive(true)
                .build();

        when(shortLinkRepository.findByShortCode("dbKey")).thenReturn(Optional.of(dbLink));

        CachedLinkDto result = cacheService.getResolvedLink("dbKey");

        assertNotNull(result);
        assertEquals("https://from-database.com", result.getOriginalUrl());
        verify(valueOperations).set(eq("link:dbKey"), any(CachedLinkDto.class), any(Duration.class));
        verify(l1Cache).put(eq("dbKey"), any(CachedLinkDto.class));
    }

    @Test
    @DisplayName("Cache penetration protection: nonexistent link stores sentinel and throws LinkNotFoundException")
    void testCachePenetrationProtection() {
        when(l1Cache.getIfPresent("notFound")).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("link:notFound")).thenReturn(null);
        when(shortLinkRepository.findByShortCode("notFound")).thenReturn(Optional.empty());

        assertThrows(LinkNotFoundException.class, () -> cacheService.getResolvedLink("notFound"));

        // Verifies sentinel stored in both L1 and L2 with 60s TTL
        verify(l1Cache).put(eq("notFound"), argThat(CachedLinkDto::isSentinel));
        verify(valueOperations).set(eq("link:notFound"), argThat(arg -> ((CachedLinkDto) arg).isSentinel()), eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("Graceful degradation: Redis connection failure falls back to database cleanly")
    void testRedisOutageFallback() {
        when(l1Cache.getIfPresent("redisDownKey")).thenReturn(null);
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis connection refused"));

        ShortLink dbLink = ShortLink.builder()
                .id(4L)
                .shortCode("redisDownKey")
                .originalUrl("https://fallback.com")
                .isActive(true)
                .build();

        when(shortLinkRepository.findByShortCode("redisDownKey")).thenReturn(Optional.of(dbLink));

        CachedLinkDto result = cacheService.getResolvedLink("redisDownKey");

        assertNotNull(result);
        assertEquals("https://fallback.com", result.getOriginalUrl());
        verify(shortLinkRepository).findByShortCode("redisDownKey");
    }
}
