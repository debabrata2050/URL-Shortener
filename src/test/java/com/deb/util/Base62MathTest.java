package com.deb.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class Base62MathTest {

    @Test
    @DisplayName("Encode zero returns '0'")
    void testEncodeZero() {
        assertEquals("0", Base62Math.encode(0));
    }

    @Test
    @DisplayName("Encode negative value throws IllegalArgumentException")
    void testEncodeNegativeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Base62Math.encode(-1));
    }

    @Test
    @DisplayName("Encode known boundary numbers")
    void testEncodeKnownValues() {
        assertEquals("1", Base62Math.encode(1));
        assertEquals("9", Base62Math.encode(9));
        assertEquals("A", Base62Math.encode(10));
        assertEquals("Z", Base62Math.encode(35));
        assertEquals("a", Base62Math.encode(36));
        assertEquals("z", Base62Math.encode(61));
        assertEquals("10", Base62Math.encode(62));
        assertEquals("11", Base62Math.encode(63));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 42L, 62L, 1000L, 916132832L, 1234567890123L, Long.MAX_VALUE})
    @DisplayName("Bidirectional symmetry: decode(encode(n)) == n")
    void testBidirectionalSymmetry(long number) {
        String encoded = Base62Math.encode(number);
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());

        long decoded = Base62Math.decode(encoded);
        assertEquals(number, decoded);
    }

    @Test
    @DisplayName("Decode invalid characters throws IllegalArgumentException")
    void testDecodeInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> Base62Math.decode("abc-123"));
        assertThrows(IllegalArgumentException.class, () -> Base62Math.decode("test/url"));
        assertThrows(IllegalArgumentException.class, () -> Base62Math.decode("hello@world"));
        assertThrows(IllegalArgumentException.class, () -> Base62Math.decode(" space "));
    }

    @Test
    @DisplayName("Decode null or empty string throws IllegalArgumentException")
    void testDecodeNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> Base62Math.decode(null));
        assertThrows(IllegalArgumentException.class, () -> Base62Math.decode(""));
    }

    @Test
    @DisplayName("Validate Base62 character constraint")
    void testIsValid() {
        assertTrue(Base62Math.isValid("0"));
        assertTrue(Base62Math.isValid("abcXYZ123"));
        assertFalse(Base62Math.isValid(""));
        assertFalse(Base62Math.isValid(null));
        assertFalse(Base62Math.isValid("invalid-slug"));
    }
}
