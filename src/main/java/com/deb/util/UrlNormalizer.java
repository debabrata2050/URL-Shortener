package com.deb.util;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Normalizes and canonicalizes URLs to ensure identical web destinations
 * produce identical short links (e.g., 'https://github.com' vs 'https://github.com/').
 */
@Component
public class UrlNormalizer {

    public String normalize(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return rawUrl;
        }

        String trimmed = rawUrl.trim();

        // Strip trailing empty '?' or '#'
        while (trimmed.endsWith("?") || trimmed.endsWith("#")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        try {
            URI uri = new URI(trimmed).normalize();

            // 1. Lowercase scheme
            String scheme = uri.getScheme();
            if (scheme != null) {
                scheme = scheme.toLowerCase();
            }

            // 2. Lowercase host
            String host = uri.getHost();
            if (host != null) {
                host = host.toLowerCase();
            }

            // 3. Strip standard default ports (80 for http, 443 for https)
            int port = uri.getPort();
            if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
                port = -1;
            }

            // 4. Normalize path:
            // - Collapse consecutive slashes ('//' -> '/')
            // - Strip root trailing slash ('https://github.com/' -> 'https://github.com')
            // - Strip directory trailing slash ('https://github.com/path/' -> 'https://github.com/path')
            String path = uri.getPath();
            if (path != null) {
                path = path.replaceAll("/{2,}", "/");
                if ("/".equals(path) || path.isEmpty()) {
                    path = "";
                } else if (path.length() > 1 && path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
            } else {
                path = "";
            }

            String userInfo = uri.getUserInfo();
            String query = uri.getQuery();
            String fragment = uri.getFragment();

            URI canonical = new URI(scheme, userInfo, host, port, path, query, fragment);
            return canonical.toString();
        } catch (URISyntaxException e) {
            return trimmed;
        }
    }
}
