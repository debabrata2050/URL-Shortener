package com.deb.validator;

import com.deb.exception.ReservedSlugException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates custom vanity slugs against regex rules and reserved system paths.
 */
@Component
public class SlugValidator {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 32;

    // Must contain only alphanumeric, hyphens, and underscores
    private static final Pattern ALLOWED_CHARS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    // Must start and end with an alphanumeric character
    private static final Pattern BOUNDARY_PATTERN = Pattern.compile("^[a-zA-Z0-9].*[a-zA-Z0-9]$");

    // Cannot contain consecutive hyphens or underscores (e.g., "--", "__", "-_", "_-")
    private static final Pattern CONSECUTIVE_SEPARATORS_PATTERN = Pattern.compile("[-_]{2,}");

    private static final Set<String> RESERVED_SLUGS = Set.of(
            "api", "admin", "r", "metrics", "actuator", "health", "info",
            "static", "index", "login", "logout", "swagger-ui", "v3",
            "error", "favicon.ico", "robots.txt", "sitemap.xml", "d50",
            "auth", "oauth", "register", "signup", "signin", "dashboard",
            "settings", "profile", "terms", "privacy", "docs", "help",
            "support", "status", "webhook", "null", "undefined", "true", "false", "root"
    );

    public void validateSlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            throw new ReservedSlugException("Slug cannot be empty");
        }

        String trimmedSlug = slug.trim();

        if (trimmedSlug.length() < MIN_LENGTH || trimmedSlug.length() > MAX_LENGTH) {
            throw new ReservedSlugException("Slug length must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters.");
        }

        if (!ALLOWED_CHARS_PATTERN.matcher(trimmedSlug).matches()) {
            throw new ReservedSlugException("Slug '" + trimmedSlug + "' contains invalid characters. Only alphanumeric, hyphens, and underscores are allowed.");
        }

        if (!BOUNDARY_PATTERN.matcher(trimmedSlug).matches()) {
            throw new ReservedSlugException("Slug '" + trimmedSlug + "' cannot start or end with a hyphen or underscore.");
        }

        if (CONSECUTIVE_SEPARATORS_PATTERN.matcher(trimmedSlug).find()) {
            throw new ReservedSlugException("Slug '" + trimmedSlug + "' cannot contain consecutive hyphens or underscores.");
        }

        if (RESERVED_SLUGS.contains(trimmedSlug.toLowerCase())) {
            throw new ReservedSlugException("Slug '" + trimmedSlug + "' is a reserved system keyword.");
        }
    }
}
