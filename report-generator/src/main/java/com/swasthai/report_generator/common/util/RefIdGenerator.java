package com.swasthai.report_generator.common.util;

import java.security.SecureRandom;

/**
 * Generates cryptographically strong, URL-safe, compact public reference IDs.
 * Uses Base62 characters [0-9A-Za-z] with 12 characters length (~71 bits of entropy),
 * eliminating the birthday collision vulnerability of 8-character hex strings.
 */
public final class RefIdGenerator {

    private static final String BASE62_CHARS =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int DEFAULT_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RefIdGenerator() {
    }

    public static String generate(String prefix) {
        StringBuilder builder = new StringBuilder(prefix.length() + 1 + DEFAULT_LENGTH);
        builder.append(prefix).append("-");

        for (int i = 0; i < DEFAULT_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(BASE62_CHARS.length());
            builder.append(BASE62_CHARS.charAt(index));
        }

        return builder.toString();
    }
}
