package com.zhs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameAccountNormalizerTest {

    @Test
    void normalize_trimsAndLowercases() {
        assertEquals("player123", GameAccountNormalizer.normalize("  Player123  "));
    }

    @Test
    void normalize_returnsNullForBlank() {
        assertNull(GameAccountNormalizer.normalize(null));
        assertNull(GameAccountNormalizer.normalize(""));
        assertNull(GameAccountNormalizer.normalize("   "));
    }

    @Test
    void display_trimsWithoutLowercasing() {
        assertEquals("Player123", GameAccountNormalizer.display("  Player123  "));
    }

    @Test
    void display_returnsNullForBlank() {
        assertNull(GameAccountNormalizer.display(null));
        assertNull(GameAccountNormalizer.display("   "));
    }
}
