package com.deb.service;

import com.deb.dto.AnalyticsSummaryResponse;
import com.deb.dto.CountryStatsResponse;
import com.deb.dto.DeviceStatsResponse;
import com.deb.dto.ReferrerStatsResponse;
import com.deb.dto.projection.CountryStats;
import com.deb.dto.projection.DeviceStats;
import com.deb.dto.projection.ReferrerStats;
import com.deb.entity.ShortLink;
import com.deb.repository.ClickEventRepository;
import com.deb.repository.ShortLinkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("getSummary calculates click counts and bot ratio percentage")
    void testGetSummary() {
        ShortLink link = ShortLink.builder()
                .id(1L)
                .shortCode("statsKey")
                .originalUrl("https://example.com")
                .build();

        when(shortLinkRepository.findByShortCode("statsKey")).thenReturn(Optional.of(link));
        when(clickEventRepository.countByShortLinkId(1L)).thenReturn(100L);
        when(clickEventRepository.countByShortLinkIdAndIsBotTrue(1L)).thenReturn(25L);
        when(clickEventRepository.countDistinctIpByShortLinkId(1L)).thenReturn(60L);

        AnalyticsSummaryResponse summary = analyticsService.getSummary("statsKey");

        assertNotNull(summary);
        assertEquals(100L, summary.getTotalClicks());
        assertEquals(25L, summary.getBotClicks());
        assertEquals(75L, summary.getHumanClicks());
        assertEquals(60L, summary.getUniqueIps());
        assertEquals(25.00, summary.getBotRatioPercentage());
    }

    @Test
    @DisplayName("getTopReferrers calculates domain click percentages")
    void testGetTopReferrers() {
        ShortLink link = ShortLink.builder()
                .id(2L)
                .shortCode("refKey")
                .originalUrl("https://example.com")
                .build();

        when(shortLinkRepository.findByShortCode("refKey")).thenReturn(Optional.of(link));
        when(clickEventRepository.countByShortLinkId(2L)).thenReturn(200L);

        ReferrerStats ref1 = createReferrerProjection("twitter.com", 100L);
        ReferrerStats ref2 = createReferrerProjection("linkedin.com", 50L);

        when(clickEventRepository.findTopReferrers(eq(2L), any(Pageable.class)))
                .thenReturn(List.of(ref1, ref2));

        List<ReferrerStatsResponse> result = analyticsService.getTopReferrers("refKey", 5);

        assertEquals(2, result.size());
        assertEquals("twitter.com", result.get(0).getReferrerDomain());
        assertEquals(100L, result.get(0).getTotalClicks());
        assertEquals(50.00, result.get(0).getPercentage());

        assertEquals("linkedin.com", result.get(1).getReferrerDomain());
        assertEquals(50L, result.get(1).getTotalClicks());
        assertEquals(25.00, result.get(1).getPercentage());
    }

    @Test
    @DisplayName("getTopCountries calculates country distribution percentages")
    void testGetTopCountries() {
        ShortLink link = ShortLink.builder()
                .id(3L)
                .shortCode("geoKey")
                .originalUrl("https://example.com")
                .build();

        when(shortLinkRepository.findByShortCode("geoKey")).thenReturn(Optional.of(link));
        when(clickEventRepository.countByShortLinkId(3L)).thenReturn(50L);

        CountryStats c1 = createCountryProjection("US", 30L);
        CountryStats c2 = createCountryProjection("IN", 20L);

        when(clickEventRepository.findTopCountries(eq(3L), any(Pageable.class)))
                .thenReturn(List.of(c1, c2));

        List<CountryStatsResponse> result = analyticsService.getTopCountries("geoKey", 5);

        assertEquals(2, result.size());
        assertEquals("US", result.get(0).getCountryCode());
        assertEquals(60.00, result.get(0).getPercentage());
        assertEquals("IN", result.get(1).getCountryCode());
        assertEquals(40.00, result.get(1).getPercentage());
    }

    @Test
    @DisplayName("getDeviceBreakdown calculates device distribution")
    void testGetDeviceBreakdown() {
        ShortLink link = ShortLink.builder()
                .id(4L)
                .shortCode("devKey")
                .originalUrl("https://example.com")
                .build();

        when(shortLinkRepository.findByShortCode("devKey")).thenReturn(Optional.of(link));
        when(clickEventRepository.countByShortLinkId(4L)).thenReturn(80L);

        DeviceStats d1 = createDeviceProjection("MOBILE", 60L);
        DeviceStats d2 = createDeviceProjection("DESKTOP", 20L);

        when(clickEventRepository.findDeviceBreakdown(4L)).thenReturn(List.of(d1, d2));

        List<DeviceStatsResponse> result = analyticsService.getDeviceBreakdown("devKey");

        assertEquals(2, result.size());
        assertEquals("MOBILE", result.get(0).getDeviceType());
        assertEquals(75.00, result.get(0).getPercentage());
        assertEquals("DESKTOP", result.get(1).getDeviceType());
        assertEquals(25.00, result.get(1).getPercentage());
    }

    private ReferrerStats createReferrerProjection(String domain, Long clicks) {
        return new ReferrerStats() {
            @Override
            public String getReferrerDomain() {
                return domain;
            }

            @Override
            public Long getTotalClicks() {
                return clicks;
            }
        };
    }

    private CountryStats createCountryProjection(String code, Long clicks) {
        return new CountryStats() {
            @Override
            public String getCountryCode() {
                return code;
            }

            @Override
            public Long getTotalClicks() {
                return clicks;
            }
        };
    }

    private DeviceStats createDeviceProjection(String type, Long clicks) {
        return new DeviceStats() {
            @Override
            public String getDeviceType() {
                return type;
            }

            @Override
            public Long getTotalClicks() {
                return clicks;
            }
        };
    }
}
