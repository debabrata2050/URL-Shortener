package com.deb.controller;

import com.deb.dto.CreateRedirectRequest;
import com.deb.dto.RedirectResponse;
import com.deb.entity.Redirect;
import com.deb.service.RedirectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
public class RedirectController {

    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);
    private final RedirectService redirectService;

    @Value("${app.base-url}")
    private String appBaseUrl;

    private static final String REDIRECT_PREFIX = "/r/"; // Consistent with your setup

    @Autowired
    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    @GetMapping("/r/{alias}")
    public ResponseEntity<?> handleRedirect(@PathVariable String alias) {
        try {
            Redirect redirect = redirectService.getRedirect(alias);
            log.info("Redirecting alias '{}' to URL: {}", alias, redirect.getUrl());
            URI uri = new URI(redirect.getUrl());
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setLocation(uri);
            // Use 301 for permanent redirect
            return new ResponseEntity<>(httpHeaders, HttpStatus.MOVED_PERMANENTLY);
        } catch (URISyntaxException e) {
            log.error("Invalid URL syntax stored for alias '{}': {}", alias, e.getMessage());
            // This should ideally not happen if URL validation works, but good to handle
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Invalid redirect URL configured.");
        }
        // NotFoundException will be handled by the GlobalExceptionHandler
    }

    @PostMapping("/api/shorten")
    public ResponseEntity<RedirectResponse> createRedirect(@Valid @RequestBody CreateRedirectRequest request) {
        log.info("Received request to create redirect: URL={}, CustomAlias='{}'", request.getUrl(), request.getAlias());
        Redirect newRedirect = redirectService.createRedirect(request);

        // --- Construct the full short URL using the injected base URL ---
        String fullShortUrl = appBaseUrl + REDIRECT_PREFIX + newRedirect.getAlias();
        log.info("Constructed full short URL: {}", fullShortUrl);

        // The RedirectResponse DTO now only needs the alias and original URL.
        // The frontend will display the full URL constructed here or receive it.
        // Let's adjust the Response DTO to carry the full short URL instead.

        RedirectResponse response = new RedirectResponse(
                newRedirect.getAlias(),       // Keep the alias if needed separately
                newRedirect.getUrl(),         // Original long URL
                fullShortUrl                  // The complete short URL to display/copy
        );


        log.info("Successfully created redirect: Alias={}, URL={}, ShortURL={}",
                response.getAlias(), response.getOriginalUrl(), response.getShortUrl());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}