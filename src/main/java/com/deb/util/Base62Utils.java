package com.deb.util;

import java.security.SecureRandom;

public class Base62Utils {

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom random = new SecureRandom();
    private static final int DEFAULT_LENGTH = 5;

    /**
     * Generates a random Base62 string of a specified length.
     * @param length The desired length of the string.
     * @return A random Base62 string.
     */
    public static String generateRandomAlias(int length) {
        if (length <= 0) {
            length = DEFAULT_LENGTH;
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_CHARS.charAt(random.nextInt(BASE62_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a random Base62 string of default length.
     * @return A random Base62 string.
     */
    public static String generateRandomAlias() {
        return generateRandomAlias(DEFAULT_LENGTH);
    }

    // Optional: If you want to encode a number (like the ID) to Base62
    public static String encodeToBase62(long n) {
        if (n == 0) {
            return String.valueOf(BASE62_CHARS.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            sb.insert(0, BASE62_CHARS.charAt((int) (n % 62)));
            n /= 62;
        }
        return sb.toString();
    }
}