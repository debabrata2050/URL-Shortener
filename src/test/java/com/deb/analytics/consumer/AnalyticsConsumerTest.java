package com.deb.analytics.consumer;

import com.deb.analytics.enricher.BotFilter;
import com.deb.analytics.enricher.GeoLocationService;
import com.deb.analytics.enricher.UserAgentParser;
import com.deb.analytics.model.ClickEventPayload;
import com.deb.entity.ClickEvent;
import com.deb.entity.ShortLink;
import com.deb.repository.ClickEventRepository;
import com.deb.repository.DailyLinkStatsRepository;
import com.deb.repository.ShortLinkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsConsumerTest {

    @Mock
    private BotFilter botFilter;

    @Mock
    private UserAgentParser userAgentParser;

    @Mock
    private GeoLocationService geoLocationService;

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private DailyLinkStatsRepository dailyLinkStatsRepository;

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @InjectMocks
    private AnalyticsConsumer consumer;

    @Test
    @DisplayName("onMessage enriches click event and buffers for micro-batch flush")
    void testOnMessageAndFlushBatch() {
        ShortLink link = ShortLink.builder()
                .id(10L)
                .shortCode("crawlKey")
                .originalUrl("https://example.com")
                .build();

        when(shortLinkRepository.findById(10L)).thenReturn(Optional.of(link));
        when(botFilter.isBot("Googlebot/2.1")).thenReturn(true);
        when(userAgentParser.parseDeviceType("Googlebot/2.1", true)).thenReturn("BOT");
        when(userAgentParser.parseBrowserFamily("Googlebot/2.1", true)).thenReturn("BOT");
        when(geoLocationService.resolve("66.249.66.1")).thenReturn(new GeoLocationService.GeoLocation("US", "North America"));

        ClickEventPayload payload = ClickEventPayload.builder()
                .linkId(10L)
                .shortCode("crawlKey")
                .clickedAt(LocalDateTime.now())
                .ipAddress("66.249.66.1")
                .userAgent("Googlebot/2.1")
                .referer("https://google.com")
                .build();

        consumer.onMessage(payload);

        assertEquals(1, consumer.getBufferSize());

        // Trigger batch flush
        consumer.flushBatch();

        assertEquals(0, consumer.getBufferSize());
        verify(clickEventRepository).saveAll(argThat(iterable -> {
            List<ClickEvent> list = (List<ClickEvent>) iterable;
            return list.size() == 1 && Boolean.TRUE.equals(list.get(0).getIsBot()) && "US".equals(list.get(0).getCountryCode());
        }));
        verify(dailyLinkStatsRepository).save(any());
    }
}
