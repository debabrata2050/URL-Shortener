package com.deb.controller;

import com.deb.dto.CreateLinkRequest;
import com.deb.dto.CreateRedirectRequest;
import com.deb.dto.LinkResponse;
import com.deb.dto.RedirectResponse;
import com.deb.service.ShortLinkService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ShortLinkController {

    private static final Logger log = LoggerFactory.getLogger(ShortLinkController.class);
    private final ShortLinkService shortLinkService;

    @PostMapping("/api/v1/links")
    public ResponseEntity<LinkResponse> createLink(@Valid @RequestBody CreateLinkRequest request) {
        log.info("REST request to create short link: originalUrl='{}', customSlug='{}'", request.getOriginalUrl(), request.getCustomSlug());
        LinkResponse response = shortLinkService.createLink(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/links")
    public ResponseEntity<List<LinkResponse>> listLinks() {
        log.info("REST request to list all short links");
        return ResponseEntity.ok(shortLinkService.listLinks());
    }

    @GetMapping("/api/v1/links/{shortCode}")
    public ResponseEntity<LinkResponse> getLink(@PathVariable String shortCode) {
        log.info("REST request to get short link details: shortCode='{}'", shortCode);
        return ResponseEntity.ok(shortLinkService.getLink(shortCode));
    }

    @PatchMapping("/api/v1/links/{shortCode}/deactivate")
    public ResponseEntity<LinkResponse> deactivateLink(@PathVariable String shortCode) {
        log.info("REST request to deactivate short link: shortCode='{}'", shortCode);
        return ResponseEntity.ok(shortLinkService.deactivateLink(shortCode));
    }

    @PutMapping("/api/v1/links/{shortCode}/destination")
    public ResponseEntity<LinkResponse> updateDestination(
            @PathVariable String shortCode,
            @RequestBody Map<String, String> body) {
        String newUrl = body.get("originalUrl");
        log.info("REST request to update destination URL: shortCode='{}', newUrl='{}'", shortCode, newUrl);
        return ResponseEntity.ok(shortLinkService.updateDestinationUrl(shortCode, newUrl));
    }

    /**
     * Backward-compatible endpoint for existing static frontend.
     */
    @PostMapping("/api/shorten")
    public ResponseEntity<RedirectResponse> legacyShorten(@Valid @RequestBody CreateRedirectRequest request) {
        log.info("REST legacy request to shorten URL: url='{}', alias='{}'", request.getUrl(), request.getAlias());
        CreateLinkRequest modernRequest = CreateLinkRequest.builder()
                .originalUrl(request.getUrl())
                .customSlug(request.getAlias())
                .build();

        LinkResponse response = shortLinkService.createLink(modernRequest);
        RedirectResponse legacyResponse = new RedirectResponse(
                response.getShortCode(),
                response.getOriginalUrl(),
                response.getShortUrl()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(legacyResponse);
    }
}
