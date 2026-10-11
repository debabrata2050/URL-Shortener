package com.deb.generator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Cryptographic random Base62 key generator.
 * Produces unpredictable tokens, requiring collision checks at persistence layer.
 */
@Component
@ConditionalOnProperty(name = "app.keygen.strategy", havingValue = "random")
public class RandomBase62KeyGenerator implements KeyGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int DEFAULT_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generateKey() {
        return generateKey(DEFAULT_LENGTH);
    }

    public String generateKey(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    @Override
    public String getStrategyName() {
        return "RANDOM_BASE62";
    }
}
