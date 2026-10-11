package com.deb.controller;

import com.deb.dto.*;
import com.deb.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/analytics/links")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;

    @GetMapping("/{shortCode}/summary")
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(@PathVariable String shortCode) {
        log.info("REST request to get analytics summary for shortCode='{}'", shortCode);
        return ResponseEntity.ok(analyticsService.getSummary(shortCode));
    }

    @GetMapping("/{shortCode}/referrers")
    public ResponseEntity<List<ReferrerStatsResponse>> getTopReferrers(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("REST request to get top {} referrers for shortCode='{}'", limit, shortCode);
        return ResponseEntity.ok(analyticsService.getTopReferrers(shortCode, limit));
    }

    @GetMapping("/{shortCode}/countries")
    public ResponseEntity<List<CountryStatsResponse>> getTopCountries(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("REST request to get top {} countries for shortCode='{}'", limit, shortCode);
        return ResponseEntity.ok(analyticsService.getTopCountries(shortCode, limit));
    }

    @GetMapping("/{shortCode}/devices")
    public ResponseEntity<List<DeviceStatsResponse>> getDeviceBreakdown(@PathVariable String shortCode) {
        log.info("REST request to get device breakdown for shortCode='{}'", shortCode);
        return ResponseEntity.ok(analyticsService.getDeviceBreakdown(shortCode));
    }
}
