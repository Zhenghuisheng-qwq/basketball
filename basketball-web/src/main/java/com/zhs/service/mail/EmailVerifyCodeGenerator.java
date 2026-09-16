package com.zhs.service.mail;

import java.security.SecureRandom;

/**
 * SecureRandom 6-digit email verification code. Always zero-padded.
 */
public final class EmailVerifyCodeGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private EmailVerifyCodeGenerator() {
    }

    public static String sixDigits() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
