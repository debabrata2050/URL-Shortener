package com.deb.analytics.consumer;

import com.deb.analytics.enricher.BotFilter;
import com.deb.analytics.enricher.GeoLocationService;
import com.deb.analytics.enricher.UserAgentParser;
import com.deb.analytics.model.ClickEventPayload;
import com.deb.config.RabbitMQConfig;
import com.deb.entity.ClickEvent;
import com.deb.entity.DailyLinkStats;
import com.deb.entity.DailyLinkStatsId;
import com.deb.entity.ShortLink;
import com.deb.repository.ClickEventRepository;
import com.deb.repository.DailyLinkStatsRepository;
import com.deb.repository.ShortLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);
    private static final int BATCH_SIZE_THRESHOLD = 50;

    private final BotFilter botFilter;
    private final UserAgentParser userAgentParser;
    private final GeoLocationService geoLocationService;
    private final ClickEventRepository clickEventRepository;
    private final DailyLinkStatsRepository dailyLinkStatsRepository;
    private final ShortLinkRepository shortLinkRepository;

    private final ConcurrentLinkedQueue<ClickEvent> buffer = new ConcurrentLinkedQueue<>();

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CLICK_EVENTS)
    public void onMessage(ClickEventPayload payload) {
        log.debug("Consumer received click event for shortCode: '{}'", payload.getShortCode());

        Optional<ShortLink> linkOpt = shortLinkRepository.findById(payload.getLinkId());
        if (linkOpt.isEmpty()) {
            log.warn("Cannot persist click event: ShortLink ID {} not found", payload.getLinkId());
            return;
        }

        ShortLink link = linkOpt.get();
        boolean isBot = botFilter.isBot(payload.getUserAgent());
        String deviceType = userAgentParser.parseDeviceType(payload.getUserAgent(), isBot);
        String browserFamily = userAgentParser.parseBrowserFamily(payload.getUserAgent(), isBot);
        GeoLocationService.GeoLocation geo = geoLocationService.resolve(payload.getIpAddress());
        String referrerDomain = extractReferrerDomain(payload.getReferer());

        ClickEvent event = ClickEvent.builder()
                .shortLink(link)
                .clickedAt(payload.getClickedAt())
                .ipAddress(payload.getIpAddress())
                .referrerDomain(referrerDomain)
                .countryCode(geo.countryCode())
                .city(geo.city())
                .deviceType(deviceType)
                .browserFamily(browserFamily)
                .isBot(isBot)
                .build();

        buffer.offer(event);

        if (buffer.size() >= BATCH_SIZE_THRESHOLD) {
            flushBatch();
        }
    }

    @Scheduled(fixedDelay = 500)
    public void scheduledFlush() {
        if (!buffer.isEmpty()) {
            flushBatch();
        }
    }

    @Transactional
    public synchronized void flushBatch() {
        if (buffer.isEmpty()) {
            return;
        }

        List<ClickEvent> batch = new ArrayList<>();
        ClickEvent event;
        while ((event = buffer.poll()) != null && batch.size() < 100) {
            batch.add(event);
        }

        if (batch.isEmpty()) {
            return;
        }

        log.info("Flushing micro-batch of {} click events to database.", batch.size());
        clickEventRepository.saveAll(batch);

        // Update daily rollups
        for (ClickEvent c : batch) {
            updateDailyStats(c);
        }
    }

    private void updateDailyStats(ClickEvent event) {
        Long linkId = event.getShortLink().getId();
        LocalDate date = event.getClickedAt().toLocalDate();
        DailyLinkStatsId id = new DailyLinkStatsId(linkId, date);

        DailyLinkStats stats = dailyLinkStatsRepository.findByIdLinkIdAndIdStatDate(linkId, date)
                .orElseGet(() -> DailyLinkStats.builder()
                        .id(id)
                        .shortLink(event.getShortLink())
                        .totalClicks(0L)
                        .uniqueIps(0L)
                        .botClicks(0L)
                        .build());

        stats.setTotalClicks(stats.getTotalClicks() + 1);
        if (Boolean.TRUE.equals(event.getIsBot())) {
            stats.setBotClicks(stats.getBotClicks() + 1);
        }

        dailyLinkStatsRepository.save(stats);
    }

    private String extractReferrerDomain(String rawReferrer) {
        if (rawReferrer == null || rawReferrer.trim().isEmpty()) {
            return "direct";
        }
        try {
            URI uri = new URI(rawReferrer.trim());
            String host = uri.getHost();
            return host != null ? host.toLowerCase().replaceFirst("^www\\.", "") : "direct";
        } catch (Exception e) {
            return "unknown";
        }
    }

    public int getBufferSize() {
        return buffer.size();
    }
}
