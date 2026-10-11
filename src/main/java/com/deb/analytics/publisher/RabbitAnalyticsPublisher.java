package com.deb.analytics.publisher;

import com.deb.analytics.model.ClickEventPayload;
import com.deb.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Resilient non-blocking analytics publisher using RabbitMQ.
 * Guarantees redirect execution latency by catching broker errors
 * and storing dropped messages into a bounded in-memory buffer.
 */
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RabbitAnalyticsPublisher implements AnalyticsEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitAnalyticsPublisher.class);
    private static final int MAX_FALLBACK_CAPACITY = 10_000;

    private final RabbitTemplate rabbitTemplate;
    private final ConcurrentLinkedQueue<ClickEventPayload> fallbackBuffer = new ConcurrentLinkedQueue<>();

    @Override
    public void publish(ClickEventPayload payload) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_CLICK_EVENTS,
                    RabbitMQConfig.ROUTING_KEY_CLICK,
                    payload
            );
            log.debug("Successfully published click event for shortCode: '{}'", payload.getShortCode());
        } catch (AmqpException e) {
            log.warn("RabbitMQ broker error while publishing click event (buffering to fallback queue): {}", e.getMessage());
            bufferFallback(payload);
        } catch (Exception e) {
            log.error("Unexpected error publishing click event: {}", e.getMessage(), e);
            bufferFallback(payload);
        }
    }

    private void bufferFallback(ClickEventPayload payload) {
        if (fallbackBuffer.size() >= MAX_FALLBACK_CAPACITY) {
            // Evict oldest record to protect memory bounds
            fallbackBuffer.poll();
        }
        fallbackBuffer.offer(payload);
    }

    @Override
    public int getFallbackBufferSize() {
        return fallbackBuffer.size();
    }

    public ConcurrentLinkedQueue<ClickEventPayload> getFallbackBuffer() {
        return fallbackBuffer;
    }
}
