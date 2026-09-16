package com.zhs.service.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailVerificationMailContentTest {

    @Test
    void htmlContainsCodeAndExpiryWithoutSecrets() {
        String html = EmailVerificationMailContent.html("482731", 5);
        assertEquals("\u3010CNPA\u3011\u90ae\u7bb1\u9a8c\u8bc1\u7801", EmailVerificationMailContent.SUBJECT);
        assertTrue(html.contains("482731"));
        assertTrue(html.contains("5"));
        assertTrue(html.contains("CNPA"));
        assertTrue(html.contains("32px"));
        assertFalse(html.contains("123456"));
        assertFalse(html.contains("token"));
        assertFalse(html.contains("password"));
        assertFalse(html.contains("member:email:code"));
    }
}
