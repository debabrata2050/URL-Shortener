package com.deb.config;

import com.deb.cache.model.CachedLinkDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RedisConfigTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testSerialization() {
        RedisSerializer<Object> serializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();
        assertNotNull(serializer);

        CachedLinkDto dto = CachedLinkDto.builder()
                .id(1L)
                .shortCode("abc1234")
                .originalUrl("https://example.com")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .isActive(true)
                .isSentinel(false)
                .build();

        byte[] bytes = serializer.serialize(dto);
        assertNotNull(bytes);

        Object deserialized = serializer.deserialize(bytes);
        assertNotNull(deserialized);
        assertTrue(deserialized instanceof CachedLinkDto);
        CachedLinkDto resultDto = (CachedLinkDto) deserialized;
        assertEquals("abc1234", resultDto.getShortCode());
        assertEquals("https://example.com", resultDto.getOriginalUrl());
    }
}
