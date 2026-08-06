package com.zhs.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameTerminalSupportTest {

    @Test
    void normalize_mapsPsVariantsToPs() {
        assertEquals("PS", GameTerminalSupport.normalize("PS5"));
        assertEquals("PS", GameTerminalSupport.normalize("psn"));
        assertEquals("PC", GameTerminalSupport.normalize("pc"));
    }

    @Test
    void matches_treatsPsAndPs5AsSamePlatform() {
        assertTrue(GameTerminalSupport.matches("PS", "PS5"));
        assertTrue(GameTerminalSupport.matches("PC", "pc"));
        assertFalse(GameTerminalSupport.matches("PC", "PS"));
        assertFalse(GameTerminalSupport.matches("MOBILE", "PC"));
    }

    @Test
    void buildApplyJoinMismatchMessage_returnsDetailedReason() {
        String message = GameTerminalSupport.buildApplyJoinMismatchMessage("PC", "PS");
        assertEquals("当前球员比赛终端为「PC」，不能申请加入「PS（历史）」球队", message);
    }

    @Test
    void buildInviteMismatchMessage_returnsDetailedReason() {
        String message = GameTerminalSupport.buildInviteMismatchMessage("PS", "PC");
        assertEquals("该球员比赛终端为「PS（历史）」，当前球队比赛终端为「PC」，无法邀请", message);
    }

    @Test
    void resolvePlayerTeamMismatchReason_returnsNullWhenMatched() {
        assertNull(GameTerminalSupport.resolvePlayerTeamMismatchReason("PC", "PC"));
        assertNull(GameTerminalSupport.resolvePlayerTeamMismatchReason("PS5", "PS"));
    }
}
