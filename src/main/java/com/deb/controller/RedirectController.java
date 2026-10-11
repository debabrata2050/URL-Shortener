package com.deb.controller;

import com.deb.analytics.model.ClickEventPayload;
import com.deb.analytics.publisher.AnalyticsEventPublisher;
import com.deb.cache.TwoTierCacheService;
import com.deb.cache.model.CachedLinkDto;
import com.deb.exception.LinkExpiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);

    private final TwoTierCacheService twoTierCacheService;
    private final AnalyticsEventPublisher analyticsEventPublisher;

    /**
     * Resolves short link and issues an immediate HTTP 302 Found redirect.
     * Extracts request headers and dispatches an asynchronous analytics event.
     */
    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> handleRedirect(@PathVariable String shortCode, HttpServletRequest request) {
        log.info("Incoming redirect request for shortCode='{}'", shortCode);
        CachedLinkDto link = twoTierCacheService.getResolvedLink(shortCode);

        // Check if link is deactivated or expired
        if (Boolean.FALSE.equals(link.getIsActive()) || link.isExpired()) {
            log.info("Redirect requested for expired or inactive shortCode: '{}'", shortCode);
            throw new LinkExpiredException("Short link '" + shortCode + "' has expired or been deactivated.");
        }

        // Asynchronously dispatch lightweight analytics event
        dispatchAnalyticsEvent(link, request);

        log.debug("Redirecting shortCode '{}' to URL '{}' (HTTP 302 Found)", shortCode, link.getOriginalUrl());

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(link.getOriginalUrl()));
        // Enforce no-cache on redirection response to prevent browser client-side caching
        headers.setCacheControl("no-cache, no-store, max-age=0, must-revalidate");
        headers.setPragma("no-cache");

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    private void dispatchAnalyticsEvent(CachedLinkDto link, HttpServletRequest request) {
        try {
            String clientIp = extractClientIp(request);
            String userAgent = request.getHeader("User-Agent");
            String referer = request.getHeader("Referer");

            ClickEventPayload payload = ClickEventPayload.builder()
                    .linkId(link.getId())
                    .shortCode(link.getShortCode())
                    .clickedAt(LocalDateTime.now())
                    .ipAddress(clientIp)
                    .userAgent(userAgent)
                    .referer(referer)
                    .build();

            analyticsEventPublisher.publish(payload);
        } catch (Exception e) {
            log.error("Failed to enqueue click analytics event for shortCode '{}': {}", link.getShortCode(), e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}