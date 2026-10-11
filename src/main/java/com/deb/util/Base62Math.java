package com.deb.util;

import java.util.Arrays;

/**
 * High-performance, zero-dependency Base62 utility using pure arithmetic
 * and pre-computed lookup tables for fast bidirectional conversion.
 */
public final class Base62Math {

    private static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final char[] ENCODE_TABLE = BASE62_ALPHABET.toCharArray();
    private static final int BASE = 62;

    // Fast ASCII lookup array for decoding (128 elements)
    private static final int[] DECODE_TABLE = new int[128];

    static {
        Arrays.fill(DECODE_TABLE, -1);
        for (int i = 0; i < ENCODE_TABLE.length; i++) {
            DECODE_TABLE[ENCODE_TABLE[i]] = i;
        }
    }

    private Base62Math() {
        // Utility class
    }

    /**
     * Encodes a non-negative 64-bit integer into a Base62 string.
     *
     * @param number non-negative number to encode
     * @return Base62 string representation
     * @throws IllegalArgumentException if number is negative
     */
    public static String encode(long number) {
        if (number < 0) {
            throw new IllegalArgumentException("Number must be non-negative: " + number);
        }
        if (number == 0) {
            return "0";
        }

        // Long.MAX_VALUE in Base62 is maximum 11 characters
        char[] buffer = new char[12];
        int charPos = buffer.length;

        long remainder = number;
        while (remainder > 0) {
            int digit = (int) (remainder % BASE);
            buffer[--charPos] = ENCODE_TABLE[digit];
            remainder /= BASE;
        }

        return new String(buffer, charPos, buffer.length - charPos);
    }

    /**
     * Decodes a Base62 string back into a 64-bit integer.
     *
     * @param str Base62 string
     * @return decoded non-negative integer
     * @throws IllegalArgumentException if str is null, empty, contains invalid characters, or overflows long
     */
    public static long decode(String str) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("Base62 string cannot be null or empty");
        }

        long result = 0L;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 128 || DECODE_TABLE[c] == -1) {
                throw new IllegalArgumentException("Invalid Base62 character '" + c + "' in string: " + str);
            }

            int digit = DECODE_TABLE[c];

            // Check overflow before multiplying
            if (result > (Long.MAX_VALUE - digit) / BASE) {
                throw new IllegalArgumentException("Base62 string causes 64-bit integer overflow: " + str);
            }

            result = result * BASE + digit;
        }

        return result;
    }

    /**
     * Validates whether a string consists entirely of valid Base62 characters.
     */
    public static boolean isValid(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= 128 || DECODE_TABLE[c] == -1) {
                return false;
            }
        }
        return true;
    }
}
