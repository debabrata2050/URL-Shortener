package com.deb.validator;

import com.deb.exception.ReservedSlugException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlugValidatorTest {

    private SlugValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SlugValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"promo2026", "my-project", "summer_sale", "abc", "CustomSlug-123", "v2-guide"})
    @DisplayName("Valid vanity slugs pass validation")
    void testValidSlugs(String slug) {
        assertDoesNotThrow(() -> validator.validateSlug(slug));
    }

    @ParameterizedTest
    @ValueSource(strings = {"api", "admin", "r", "metrics", "actuator", "health", "static", "login", "auth", "dashboard", "null"})
    @DisplayName("Reserved keywords throw ReservedSlugException")
    void testReservedKeywords(String slug) {
        assertThrows(ReservedSlugException.class, () -> validator.validateSlug(slug));
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "with spaces", "foo/bar", "special#char", "slug!", "name.html", "file.json"})
    @DisplayName("Invalid characters or too short slugs throw ReservedSlugException")
    void testInvalidCharacters(String slug) {
        assertThrows(ReservedSlugException.class, () -> validator.validateSlug(slug));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-leading-hyphen", "trailing-hyphen-", "_leading_underscore", "trailing_underscore_"})
    @DisplayName("Slugs starting or ending with hyphen or underscore throw ReservedSlugException")
    void testBoundarySeparators(String slug) {
        assertThrows(ReservedSlugException.class, () -> validator.validateSlug(slug));
    }

    @ParameterizedTest
    @ValueSource(strings = {"double--hyphen", "double__underscore", "mixed-_separator"})
    @DisplayName("Consecutive hyphens or underscores throw ReservedSlugException")
    void testConsecutiveSeparators(String slug) {
        assertThrows(ReservedSlugException.class, () -> validator.validateSlug(slug));
    }

    @Test
    @DisplayName("Slug exceeding 32 characters throws ReservedSlugException")
    void testSlugTooLong() {
        String longSlug = "a".repeat(33);
        assertThrows(ReservedSlugException.class, () -> validator.validateSlug(longSlug));
    }

    @Test
    @DisplayName("Null or empty slug throws ReservedSlugException")
    void testNullOrEmpty() {
        assertThrows(ReservedSlugException.class, () -> validator.validateSlug(null));
        assertThrows(ReservedSlugException.class, () -> validator.validateSlug("   "));
    }
}
