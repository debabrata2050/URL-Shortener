package com.deb.cache;

import com.deb.cache.model.CachedLinkDto;
import com.deb.config.RedisConfig;
import com.deb.entity.ShortLink;
import com.deb.exception.LinkNotFoundException;
import com.deb.repository.ShortLinkRepository;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TwoTierCacheService {

    private static final Logger log = LoggerFactory.getLogger(TwoTierCacheService.class);

    private static final String REDIS_KEY_PREFIX = "link:";
    private static final long DEFAULT_L2_TTL_HOURS = 24L;
    private static final long SENTINEL_TTL_SECONDS = 60L;

    private final Cache<String, CachedLinkDto> l1Cache;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ShortLinkRepository shortLinkRepository;

    // Striped locks to prevent cache stampede / thundering herd per key
    private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

    /**
     * Resolves short link through L1 -> L2 -> L3 hierarchy.
     */
    public CachedLinkDto getResolvedLink(String shortCode) {
        // 1. Check L1 Cache (Caffeine Local RAM)
        CachedLinkDto l1Hit = l1Cache.getIfPresent(shortCode);
        if (l1Hit != null) {
            log.trace("L1 Cache HIT for shortCode: '{}'", shortCode);
            if (l1Hit.isSentinel()) {
                throw new LinkNotFoundException("Link not found: " + shortCode);
            }
            return l1Hit;
        }

        // 2. Check L2 Cache (Redis Distributed Cache)
        String redisKey = REDIS_KEY_PREFIX + shortCode;
        CachedLinkDto l2Hit = getFromRedis(redisKey);
        if (l2Hit != null) {
            log.trace("L2 Cache HIT for shortCode: '{}'", shortCode);
            l1Cache.put(shortCode, l2Hit);
            if (l2Hit.isSentinel()) {
                throw new LinkNotFoundException("Link not found: " + shortCode);
            }
            return l2Hit;
        }

        // 3. Fallback to L3 (MySQL Store) with single-flight locking per key
        Object lock = keyLocks.computeIfAbsent(shortCode, k -> new Object());
        synchronized (lock) {
            try {
                // Double check L1 inside lock
                CachedLinkDto doubleCheckL1 = l1Cache.getIfPresent(shortCode);
                if (doubleCheckL1 != null) {
                    if (doubleCheckL1.isSentinel()) {
                        throw new LinkNotFoundException("Link not found: " + shortCode);
                    }
                    return doubleCheckL1;
                }

                log.debug("L1/L2 Cache MISS for shortCode: '{}'. Querying MySQL database.", shortCode);
                Optional<ShortLink> linkOpt = shortLinkRepository.findByShortCode(shortCode);

                if (linkOpt.isEmpty()) {
                    // Cache penetration protection: store negative sentinel with 60s TTL
                    CachedLinkDto sentinel = CachedLinkDto.createSentinel(shortCode);
                    l1Cache.put(shortCode, sentinel);
                    putInRedis(redisKey, sentinel, Duration.ofSeconds(SENTINEL_TTL_SECONDS));
                    throw new LinkNotFoundException("Link not found: " + shortCode);
                }

                ShortLink link = linkOpt.get();
                CachedLinkDto dto = CachedLinkDto.builder()
                        .id(link.getId())
                        .shortCode(link.getShortCode())
                        .originalUrl(link.getOriginalUrl())
                        .expiresAt(link.getExpiresAt())
                        .isActive(link.getIsActive())
                        .isSentinel(false)
                        .build();

                // Compute dynamic TTL for Redis
                Duration redisTtl = calculateDynamicTtl(link.getExpiresAt());

                // Backfill L2 and L1
                putInRedis(redisKey, dto, redisTtl);
                l1Cache.put(shortCode, dto);

                return dto;
            } finally {
                keyLocks.remove(shortCode);
            }
        }
    }

    /**
     * Evicts link from L1, L2, and broadcasts eviction via Redis Pub/Sub.
     */
    public void evict(String shortCode) {
        log.info("Evicting shortCode '{}' from L1 and L2 caches.", shortCode);
        l1Cache.invalidate(shortCode);

        String redisKey = REDIS_KEY_PREFIX + shortCode;
        try {
            redisTemplate.delete(redisKey);
            redisTemplate.convertAndSend(RedisConfig.CHANNEL_CACHE_EVICTION, shortCode);
        } catch (Exception e) {
            log.warn("Redis eviction/pubsub broadcast failed for '{}': {}", shortCode, e.getMessage());
        }
    }

    private CachedLinkDto getFromRedis(String redisKey) {
        try {
            Object val = redisTemplate.opsForValue().get(redisKey);
            if (val instanceof CachedLinkDto) {
                return (CachedLinkDto) val;
            }
        } catch (Exception e) {
            log.warn("L2 Redis read failed for key '{}' (gracefully falling back to DB): {}", redisKey, e.getMessage());
        }
        return null;
    }

    private void putInRedis(String redisKey, CachedLinkDto dto, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(redisKey, dto, timeout);
        } catch (Exception e) {
            log.warn("L2 Redis write failed for key '{}': {}", redisKey, e.getMessage());
        }
    }

    private Duration calculateDynamicTtl(LocalDateTime expiresAt) {
        long defaultSeconds = Duration.ofHours(DEFAULT_L2_TTL_HOURS).toSeconds();
        if (expiresAt == null) {
            return Duration.ofSeconds(defaultSeconds);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiresAt)) {
            return Duration.ofSeconds(1L); // Expired, purge immediately
        }

        long remainingSeconds = Duration.between(now, expiresAt).toSeconds();
        return Duration.ofSeconds(Math.min(defaultSeconds, Math.max(remainingSeconds, 1L)));
    }
}
