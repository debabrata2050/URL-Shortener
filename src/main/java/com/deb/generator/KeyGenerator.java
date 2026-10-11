package com.deb.generator;

public interface KeyGenerator {
    /**
     * Generates a unique short key identifier.
     */
    String generateKey();

    /**
     * Identifies the key generation strategy name.
     */
    String getStrategyName();
}
