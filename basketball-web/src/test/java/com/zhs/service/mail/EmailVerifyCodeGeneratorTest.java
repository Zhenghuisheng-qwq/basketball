package com.zhs.service.mail;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailVerifyCodeGeneratorTest {

    @Test
    void case1_sixDigitsAlwaysPaddedAndNumeric() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            String code = EmailVerifyCodeGenerator.sixDigits();
            assertEquals(6, code.length(), code);
            assertTrue(code.matches("^\\d{6}$"), code);
            seen.add(code);
        }
        assertTrue(seen.size() > 1, "SecureRandom should not collapse to a single value");
    }
}
