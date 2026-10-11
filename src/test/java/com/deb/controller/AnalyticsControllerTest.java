package com.deb.controller;

import com.deb.dto.AnalyticsSummaryResponse;
import com.deb.dto.CountryStatsResponse;
import com.deb.dto.DeviceStatsResponse;
import com.deb.dto.ReferrerStatsResponse;
import com.deb.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(analyticsController).build();
    }

    @Test
    @DisplayName("GET /api/v1/analytics/links/{shortCode}/summary returns summary metrics")
    void testGetSummary() throws Exception {
        AnalyticsSummaryResponse summary = AnalyticsSummaryResponse.builder()
                .shortCode("demo")
                .originalUrl("https://example.com")
                .totalClicks(150L)
                .uniqueIps(100L)
                .botClicks(30L)
                .humanClicks(120L)
                .botRatioPercentage(20.0)
                .build();

        when(analyticsService.getSummary("demo")).thenReturn(summary);

        mockMvc.perform(get("/api/v1/analytics/links/demo/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("demo"))
                .andExpect(jsonPath("$.totalClicks").value(150))
                .andExpect(jsonPath("$.botRatioPercentage").value(20.0));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/links/{shortCode}/referrers returns top referrers")
    void testGetTopReferrers() throws Exception {
        ReferrerStatsResponse ref = ReferrerStatsResponse.builder()
                .referrerDomain("twitter.com")
                .totalClicks(45L)
                .percentage(45.0)
                .build();

        when(analyticsService.getTopReferrers("demo", 5)).thenReturn(List.of(ref));

        mockMvc.perform(get("/api/v1/analytics/links/demo/referrers?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].referrerDomain").value("twitter.com"))
                .andExpect(jsonPath("$[0].percentage").value(45.0));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/links/{shortCode}/countries returns geographic breakdown")
    void testGetTopCountries() throws Exception {
        CountryStatsResponse country = CountryStatsResponse.builder()
                .countryCode("US")
                .totalClicks(80L)
                .percentage(80.0)
                .build();

        when(analyticsService.getTopCountries("demo", 5)).thenReturn(List.of(country));

        mockMvc.perform(get("/api/v1/analytics/links/demo/countries?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].countryCode").value("US"));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/links/{shortCode}/devices returns device distribution")
    void testGetDevices() throws Exception {
        DeviceStatsResponse device = DeviceStatsResponse.builder()
                .deviceType("MOBILE")
                .totalClicks(70L)
                .percentage(70.0)
                .build();

        when(analyticsService.getDeviceBreakdown("demo")).thenReturn(List.of(device));

        mockMvc.perform(get("/api/v1/analytics/links/demo/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deviceType").value("MOBILE"));
    }
}
