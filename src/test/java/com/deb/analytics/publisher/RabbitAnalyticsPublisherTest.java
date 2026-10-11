package com.deb.analytics.publisher;

import com.deb.analytics.model.ClickEventPayload;
import com.deb.config.RabbitMQConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;

import java.net.ConnectException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitAnalyticsPublisherTest {

    @Mock
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitAnalyticsPublisher publisher;

    @Test
    @DisplayName("Healthy broker publishes message to RabbitMQ exchange")
    void testPublishHealthyBroker() {
        ClickEventPayload payload = ClickEventPayload.builder()
                .linkId(1L)
                .shortCode("demoKey")
                .clickedAt(LocalDateTime.now())
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .referer("https://twitter.com")
                .build();

        assertDoesNotThrow(() -> publisher.publish(payload));

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_CLICK_EVENTS),
                eq(RabbitMQConfig.ROUTING_KEY_CLICK),
                eq(payload)
        );
        assertEquals(0, publisher.getFallbackBufferSize());
    }

    @Test
    @DisplayName("Broker connection failure buffers message in fallback queue without throwing")
    void testPublishBrokerFailureBuffersPayload() {
        ClickEventPayload payload = ClickEventPayload.builder()
                .linkId(2L)
                .shortCode("failoverKey")
                .clickedAt(LocalDateTime.now())
                .build();

        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(Object.class));

        // Must not throw exception — non-blocking redirect guarantee!
        assertDoesNotThrow(() -> publisher.publish(payload));

        assertEquals(1, publisher.getFallbackBufferSize());
        assertEquals(payload, publisher.getFallbackBuffer().peek());
    }
}
