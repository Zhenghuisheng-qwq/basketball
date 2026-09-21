package com.zhs.basket.support;

import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalUrlValidatorTest {

    @Test
    void emptyUrlIsAllowed() {
        assertDoesNotThrow(() -> ExternalUrlValidator.validateHttpUrl(null, "跳转链接"));
        assertDoesNotThrow(() -> ExternalUrlValidator.validateHttpUrl("", "跳转链接"));
        assertDoesNotThrow(() -> ExternalUrlValidator.validateHttpUrl("   ", "跳转链接"));
    }

    @Test
    void httpAndHttpsAreAllowed() {
        assertDoesNotThrow(() -> ExternalUrlValidator.validateHttpUrl("https://kook.top/cnpa", "跳转链接"));
        assertDoesNotThrow(() -> ExternalUrlValidator.validateHttpUrl("http://example.org/group", "跳转链接"));
    }

    @Test
    void javascriptIsRejected() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> ExternalUrlValidator.validateHttpUrl("javascript:alert(1)", "跳转链接"));
        assertTrue(ex.getMessage().contains("javascript"));
    }

    @Test
    void dataFileAndVbscriptAreRejected() {
        assertThrows(ServiceException.class,
                () -> ExternalUrlValidator.validateHttpUrl("data:text/html,hi", "跳转链接"));
        assertThrows(ServiceException.class,
                () -> ExternalUrlValidator.validateHttpUrl("file:///etc/passwd", "跳转链接"));
        assertThrows(ServiceException.class,
                () -> ExternalUrlValidator.validateHttpUrl("vbscript:msgbox(1)", "跳转链接"));
    }

    @Test
    void lengthLimitIsEnforced() {
        String tooLong = "https://kook.top/" + "a".repeat(500);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> ExternalUrlValidator.validateHttpUrl(tooLong, "跳转链接", 512));
        assertTrue(ex.getMessage().contains("长度"));
    }
}
