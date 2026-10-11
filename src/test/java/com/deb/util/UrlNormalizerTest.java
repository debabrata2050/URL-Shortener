package com.deb.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UrlNormalizerTest {

    private UrlNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new UrlNormalizer();
    }

    @Test
    @DisplayName("Root URLs with and without trailing slash normalize to identical string")
    void testRootUrlTrailingSlash() {
        String withSlash = normalizer.normalize("https://github.com/");
        String withoutSlash = normalizer.normalize("https://github.com");

        assertEquals("https://github.com", withSlash);
        assertEquals("https://github.com", withoutSlash);
        assertEquals(withoutSlash, withSlash);
    }

    @Test
    @DisplayName("Path URLs with and without trailing slash normalize to identical string")
    void testPathUrlTrailingSlash() {
        String withSlash = normalizer.normalize("https://github.com/torvalds/linux/");
        String withoutSlash = normalizer.normalize("https://github.com/torvalds/linux");

        assertEquals("https://github.com/torvalds/linux", withSlash);
        assertEquals("https://github.com/torvalds/linux", withoutSlash);
        assertEquals(withoutSlash, withSlash);
    }

    @ParameterizedTest
    @CsvSource({
            "'HTTPS://GITHUB.COM', 'https://github.com'",
            "'Http://Example.Com/Docs', 'http://example.com/Docs'",
            "'HTTPS://Example.COM:443/', 'https://example.com'",
            "'http://example.com:80/api', 'http://example.com/api'",
            "'http://example.com:8080/api/', 'http://example.com:8080/api'",
            "'https://github.com//foo///bar', 'https://github.com/foo/bar'",
            "'https://github.com/a/b/../c', 'https://github.com/a/c'",
            "'https://github.com/./a', 'https://github.com/a'",
            "'https://github.com/?', 'https://github.com'",
            "'https://github.com/#', 'https://github.com'",
            "'  https://github.com/   ', 'https://github.com'"
    })
    @DisplayName("URL canonicalization edge cases")
    void testCanonicalizationCases(String input, String expected) {
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    @DisplayName("Query string and fragments are preserved with casing")
    void testQueryAndFragmentPreserved() {
        String normalized = normalizer.normalize("https://github.com/search?q=Spring+Boot&type=repositories#top");
        assertEquals("https://github.com/search?q=Spring+Boot&type=repositories#top", normalized);
    }

    @Test
    @DisplayName("Null and empty inputs handled safely")
    void testNullAndEmpty() {
        assertNull(normalizer.normalize(null));
        assertEquals("", normalizer.normalize(""));
        assertEquals("   ", normalizer.normalize("   "));
    }
}
