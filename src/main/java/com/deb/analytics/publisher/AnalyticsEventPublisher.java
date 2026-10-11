package com.deb.analytics.publisher;

import com.deb.analytics.model.ClickEventPayload;

public interface AnalyticsEventPublisher {
    void publish(ClickEventPayload payload);
    int getFallbackBufferSize();
}
