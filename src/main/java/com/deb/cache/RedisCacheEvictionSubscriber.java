package com.deb.cache;

import com.deb.cache.model.CachedLinkDto;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisCacheEvictionSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheEvictionSubscriber.class);

    private final Cache<String, CachedLinkDto> l1Cache;
    private final RedisMessageListenerContainer container;
    private final ChannelTopic evictionTopic;

    @PostConstruct
    public void registerListener() {
        try {
            container.addMessageListener(this, evictionTopic);
            log.info("Registered Redis Pub/Sub listener on channel: {}", evictionTopic.getTopic());
        } catch (Exception e) {
            log.warn("Could not register Redis Pub/Sub listener: {}", e.getMessage());
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String shortCode = new String(message.getBody(), StandardCharsets.UTF_8).replace("\"", "");
            log.info("Received cache eviction broadcast for shortCode: '{}'. Evicting from L1 Caffeine cache.", shortCode);
            l1Cache.invalidate(shortCode);
        } catch (Exception e) {
            log.error("Error processing Redis cache eviction message: {}", e.getMessage(), e);
        }
    }
}
