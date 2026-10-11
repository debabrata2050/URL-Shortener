package com.deb.validator;

import com.deb.exception.InvalidDestinationUrlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlSecurityValidatorTest {

    private UrlSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new UrlSecurityValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://example.com",
            "http://example.com/test?query=param",
            "https://github.com/torvalds/linux",
            "https://d50.in/articles/tech"
    })
    @DisplayName("Valid public HTTP/HTTPS URLs pass validation")
    void testValidPublicUrls(String url) {
        assertDoesNotThrow(() -> validator.validateUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost",
            "http://localhost:8080/admin",
            "http://127.0.0.1",
            "http://127.0.0.1:3000/internal",
            "http://0.0.0.0",
            "http://[::1]:8080"
    })
    @DisplayName("Rejects localhost and loopback addresses (SSRF defense)")
    void testRejectsLoopback(String url) {
        assertThrows(InvalidDestinationUrlException.class, () -> validator.validateUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://192.168.1.1",
            "http://10.0.0.1/sensitive",
            "http://172.16.0.1",
            "http://172.31.255.255"
    })
    @DisplayName("Rejects private RFC 1918 subnets (SSRF defense)")
    void testRejectsPrivateSubnets(String url) {
        assertThrows(InvalidDestinationUrlException.class, () -> validator.validateUrl(url));
    }

    @Test
    @DisplayName("Rejects cloud instance metadata service IP 169.254.169.254")
    void testRejectsAwsMetadata() {
        assertThrows(InvalidDestinationUrlException.class,
                () -> validator.validateUrl("http://169.254.169.254/latest/meta-data/"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://files.example.com/data",
            "file:///etc/passwd",
            "javascript:alert(1)",
            "data:text/html,<h1>test</h1>"
    })
    @DisplayName("Rejects non-HTTP schemes")
    void testRejectsNonHttpSchemes(String url) {
        assertThrows(InvalidDestinationUrlException.class, () -> validator.validateUrl(url));
    }

    @Test
    @DisplayName("Rejects empty or null URL")
    void testRejectsEmptyOrNull() {
        assertThrows(InvalidDestinationUrlException.class, () -> validator.validateUrl(null));
        assertThrows(InvalidDestinationUrlException.class, () -> validator.validateUrl("   "));
    }
}
