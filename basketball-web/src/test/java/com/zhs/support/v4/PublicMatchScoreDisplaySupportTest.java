package com.zhs.support.v4;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PublicMatchScoreDisplaySupportTest {

    @Test
    void bo3ChildMatches_useBasketballScore() {
        assertEquals("78:72", PublicMatchScoreDisplaySupport.childMatchScore(78, 72, null));
        assertEquals("76:71", PublicMatchScoreDisplaySupport.childMatchScore(76, 71, null));
    }

    @Test
    void forfeit_usesOfficialLabel_notZeroScore() {
        assertEquals("判胜", PublicMatchScoreDisplaySupport.childMatchScore(null, null, "判胜"));
        assertNull(PublicMatchScoreDisplaySupport.childMatchScore(null, null, null));
    }

    @Test
    void seriesScore_isNotChildMatchScore() {
        assertEquals("78:72", PublicMatchScoreDisplaySupport.childMatchScore(78, 72, null));
    }
}