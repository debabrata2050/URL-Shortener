package com.deb.service;

import com.deb.dto.*;
import com.deb.dto.projection.CountryStats;
import com.deb.dto.projection.DeviceStats;
import com.deb.dto.projection.ReferrerStats;
import com.deb.entity.ShortLink;
import com.deb.exception.LinkNotFoundException;
import com.deb.repository.ClickEventRepository;
import com.deb.repository.ShortLinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final ShortLinkRepository shortLinkRepository;
    private final ClickEventRepository clickEventRepository;

    public AnalyticsSummaryResponse getSummary(String shortCode) {
        log.debug("Calculating analytics summary for shortCode='{}'", shortCode);
        ShortLink link = findLinkOrThrow(shortCode);
        Long linkId = link.getId();

        long totalClicks = clickEventRepository.countByShortLinkId(linkId);
        long botClicks = clickEventRepository.countByShortLinkIdAndIsBotTrue(linkId);
        long uniqueIps = clickEventRepository.countDistinctIpByShortLinkId(linkId);
        long humanClicks = totalClicks - botClicks;

        double botRatio = totalClicks > 0
                ? BigDecimal.valueOf((double) botClicks / totalClicks * 100)
                .setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        AnalyticsSummaryResponse summary = AnalyticsSummaryResponse.builder()
                .shortCode(link.getShortCode())
                .originalUrl(link.getOriginalUrl())
                .totalClicks(totalClicks)
                .uniqueIps(uniqueIps)
                .botClicks(botClicks)
                .humanClicks(humanClicks)
                .botRatioPercentage(botRatio)
                .build();

        log.info("Analytics summary computed for shortCode='{}': totalClicks={}, humanClicks={}, botClicks={}",
                shortCode, totalClicks, humanClicks, botClicks);
        return summary;
    }

    public List<ReferrerStatsResponse> getTopReferrers(String shortCode, int limit) {
        log.debug("Fetching top {} referrers for shortCode='{}'", limit, shortCode);
        ShortLink link = findLinkOrThrow(shortCode);
        long totalClicks = clickEventRepository.countByShortLinkId(link.getId());

        List<ReferrerStats> projections = clickEventRepository.findTopReferrers(
                link.getId(), PageRequest.of(0, Math.max(1, limit))
        );

        List<ReferrerStatsResponse> results = projections.stream()
                .map(p -> {
                    double percentage = totalClicks > 0
                            ? BigDecimal.valueOf((double) p.getTotalClicks() / totalClicks * 100)
                            .setScale(2, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return ReferrerStatsResponse.builder()
                            .referrerDomain(p.getReferrerDomain())
                            .totalClicks(p.getTotalClicks())
                            .percentage(percentage)
                            .build();
                })
                .toList();

        log.info("Found {} top referrers for shortCode='{}'", results.size(), shortCode);
        return results;
    }

    public List<CountryStatsResponse> getTopCountries(String shortCode, int limit) {
        log.debug("Fetching top {} countries for shortCode='{}'", limit, shortCode);
        ShortLink link = findLinkOrThrow(shortCode);
        long totalClicks = clickEventRepository.countByShortLinkId(link.getId());

        List<CountryStats> projections = clickEventRepository.findTopCountries(
                link.getId(), PageRequest.of(0, Math.max(1, limit))
        );

        List<CountryStatsResponse> results = projections.stream()
                .map(p -> {
                    double percentage = totalClicks > 0
                            ? BigDecimal.valueOf((double) p.getTotalClicks() / totalClicks * 100)
                            .setScale(2, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return CountryStatsResponse.builder()
                            .countryCode(p.getCountryCode())
                            .totalClicks(p.getTotalClicks())
                            .percentage(percentage)
                            .build();
                })
                .toList();

        log.info("Found {} top countries for shortCode='{}'", results.size(), shortCode);
        return results;
    }

    public List<DeviceStatsResponse> getDeviceBreakdown(String shortCode) {
        log.debug("Fetching device breakdown for shortCode='{}'", shortCode);
        ShortLink link = findLinkOrThrow(shortCode);
        long totalClicks = clickEventRepository.countByShortLinkId(link.getId());

        List<DeviceStats> projections = clickEventRepository.findDeviceBreakdown(link.getId());

        List<DeviceStatsResponse> results = projections.stream()
                .map(p -> {
                    double percentage = totalClicks > 0
                            ? BigDecimal.valueOf((double) p.getTotalClicks() / totalClicks * 100)
                            .setScale(2, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return DeviceStatsResponse.builder()
                            .deviceType(p.getDeviceType())
                            .totalClicks(p.getTotalClicks())
                            .percentage(percentage)
                            .build();
                })
                .toList();

        log.info("Found {} device types for shortCode='{}'", results.size(), shortCode);
        return results;
    }

    private ShortLink findLinkOrThrow(String shortCode) {
        return shortLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException("Short link '" + shortCode + "' not found"));
    }
}
