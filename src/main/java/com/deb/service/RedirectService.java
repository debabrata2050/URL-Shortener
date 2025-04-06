package com.deb.service;

import com.deb.dto.CreateRedirectRequest;
import com.deb.entity.Redirect;
import com.deb.exception.AliasGenerationException;
import com.deb.exception.BadRequestException;
import com.deb.exception.NotFoundException;
import com.deb.repository.RedirectRepository;
import com.deb.util.Base62Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Service
public class RedirectService {

    private static final Logger log = LoggerFactory.getLogger(RedirectService.class);
    private static final int MAX_ALIAS_GENERATION_ATTEMPTS = 5;

    private RedirectRepository redirectRepository;

    @Autowired
    public RedirectService(RedirectRepository redirectRepository) {
        this.redirectRepository = redirectRepository;
    }


    private String normalizeUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            log.warn("Attempted to normalize null or empty URL string.");
            return urlString; // Or null, depending on desired behavior
        }

        try {
            // Trim whitespace before parsing
            URI uri = new URI(urlString.trim());

            // Rule 1: Convert scheme to lowercase (if present)
            String scheme = uri.getScheme();
            if (scheme != null) {
                scheme = scheme.toLowerCase();
            } else {
                // Scheme is mandatory for normalization rules involving default ports
                // If you expect relative URLs, this logic might need adjustment
                log.warn("URL normalization requires a scheme (http/https): {}", urlString);
                return urlString; // Return original if no scheme
            }

            // Rule 2: Convert host to lowercase (if present)
            String host = uri.getHost();
            if (host != null) {
                host = host.toLowerCase();
            } // Note: If host is null, rules involving default port or root path might not apply cleanly

            // Rule 3: Remove default port
            int port = uri.getPort();
            if (port != -1 && // Port is explicitly specified
                    (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443))) {
                port = -1; // Mark port to be excluded during reconstruction
            }

            // Rule 4 & 5: Normalize path (ensure root '/', remove trailing '/')
            String path = uri.getPath();
            if (host != null) { // Path normalization typically applies when there's a host
                if (path == null || path.isEmpty()) {
                    // Rule 4: Ensure empty path becomes "/"
                    path = "/";
                } else if (path.length() > 1 && path.endsWith("/")) {
                    // Rule 5: Remove trailing slash unless it's the root path itself
                    path = path.substring(0, path.length() - 1);
                }
                // Case: path is exactly "/" - leave it as is.
            }
            // If no host, we generally keep the path as is (e.g., mailto:test@example.com)


            // Preserve other components
            String userInfo = uri.getUserInfo();
            String query = uri.getQuery();
            String fragment = uri.getFragment();

            // Reconstruct the URI with normalized components
            URI normalizedUri = new URI(scheme, userInfo, host, port, path, query, fragment);

            String normalizedString = normalizedUri.toString();
            log.trace("Normalized '{}' to '{}'", urlString, normalizedString);
            return normalizedString;

        } catch (URISyntaxException e) {
            log.warn("Failed to parse URI for normalization, returning original URL string '{}'. Error: {}", urlString, e.getMessage());
            // Depending on requirements, you might want to throw an exception for invalid URLs
            // throw new BadRequestException("Invalid URL format provided: " + urlString);
            return urlString; // Return original if parsing fails
        } catch (Exception e) {
            // Catch any other unexpected exception during normalization
            log.error("Unexpected error during URL normalization for '{}', returning original.", urlString, e);
            return urlString;
        }
    }

    @Transactional
    public Redirect createRedirect(CreateRedirectRequest request) {
        // --- Normalize the input URL FIRST ---
        String originalUrl = request.getUrl(); // Keep original for potential logging/display
        String normalizedUrl = normalizeUrl(originalUrl);
        if (normalizedUrl == null || normalizedUrl.isEmpty()) {
            throw new BadRequestException("URL cannot be empty or null.");
        }
        log.debug("Original URL: [{}], Normalized URL: [{}]", originalUrl, normalizedUrl);
        // --- End of Normalization ---


        Optional<Redirect> existingRedirectOpt = redirectRepository.findByUrl(normalizedUrl);
        if (existingRedirectOpt.isPresent()) {
            Redirect existingRedirect = existingRedirectOpt.get();
            log.info("Normalized URL [{}] already exists with alias [{}]. Returning existing entry.", normalizedUrl, existingRedirect.getAlias());
            return existingRedirect;
        }

        log.info("Normalized URL [{}] not found. Proceeding to create new redirect.", normalizedUrl);

        String alias;
        boolean isCustomAlias = StringUtils.hasText(request.getAlias()); // Check if alias is provided and not empty

        if (isCustomAlias) {
            alias = request.getAlias().trim();
            log.info("Attempting to use custom alias: {}", alias);
            if (redirectRepository.existsByAlias(alias)) {
                log.warn("Custom alias already exists: {}", alias);
                throw new BadRequestException("Custom alias '" + alias + "' already exists.");
            }
        } else {
            // User wants a random alias
            log.info("Generating random alias for URL: {}", request.getUrl());
            int attempts = 0;
            do {
                alias = Base62Utils.generateRandomAlias(); // Generate potential alias
                attempts++;
                if (attempts > MAX_ALIAS_GENERATION_ATTEMPTS) {
                    log.error("Failed to generate unique alias after {} attempts.", MAX_ALIAS_GENERATION_ATTEMPTS);
                    throw new AliasGenerationException("Could not generate a unique alias. Please try again later.");
                }
            } while (redirectRepository.existsByAlias(alias)); // Check for collision
            log.info("Generated unique alias: {}", alias);
        }

        // Alias is unique (either custom or generated), proceed to save
        Redirect newRedirect = new Redirect(alias, normalizedUrl); // Use normalized URL here
        Redirect savedRedirect = redirectRepository.save(newRedirect);
        log.info("Saved redirect: Alias={}, URL={}, ID={}", savedRedirect.getAlias(), savedRedirect.getUrl(), savedRedirect.getId());

        return savedRedirect;
    }

    @Transactional(readOnly = true)
    public Redirect getRedirect(String alias) {
        log.debug("Attempting to find redirect for alias: {}", alias);
        return redirectRepository.findByAlias(alias)
                .orElseThrow(() -> {
                    log.warn("Alias not found: {}", alias);
                    return new NotFoundException("Alias '" + alias + "' not found.");
                });
    }
}
