package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayoffBracketSupportTest {

    @Test
    void sixteenTeamFirstRoundSeedOrder() {
        assertArrayEquals(
                new int[]{1, 16, 4, 13, 5, 12, 8, 9, 6, 11, 7, 10, 2, 15, 3, 14},
                PlayoffBracketSupport.firstRoundSeedOrder(16));
    }

    @Test
    void eightTeamFirstRoundSeedOrder() {
        assertArrayEquals(new int[]{1, 8, 4, 5, 2, 7, 3, 6}, PlayoffBracketSupport.firstRoundSeedOrder(8));
    }

    @Test
    void fourTeamFirstRoundSeedOrder() {
        assertArrayEquals(new int[]{1, 4, 2, 3}, PlayoffBracketSupport.firstRoundSeedOrder(4));
    }

    @Test
    void thirtyTwoTeamFirstRoundSeedOrderKeepsSeed1And2InOppositeHalves() {
        int[] order = PlayoffBracketSupport.firstRoundSeedOrder(32);
        assertEquals(32, order.length);
        assertEquals(1, order[0]);
        int idx1 = indexOfSeed(order, 1);
        int idx2 = indexOfSeed(order, 2);
        int idx3 = indexOfSeed(order, 3);
        int idx4 = indexOfSeed(order, 4);
        assertTrue(idx1 < 16 != idx2 < 16, "Seed1 / Seed2 必须分半区");
        assertTrue(idx1 / 8 != idx4 / 8, "Seed1 / Seed4 不能早于 Semifinal 相遇");
        assertTrue(idx1 / 16 == idx4 / 16, "Seed1 / Seed4 应在同一半区于 SF 相遇");
        assertTrue(idx2 / 8 != idx3 / 8, "Seed2 / Seed3 不能早于 Semifinal 相遇");
        assertTrue(idx2 / 16 == idx3 / 16, "Seed2 / Seed3 应在同一半区于 SF 相遇");
        for (int i = 0; i < order.length; i += 2) {
            assertEquals(33, order[i] + order[i + 1]);
        }
    }

    private static int indexOfSeed(int[] order, int seed) {
        for (int i = 0; i < order.length; i++) {
            if (order[i] == seed) {
                return i;
            }
        }
        return -1;
    }

    @Test
    void supportsPowerOfTwoTeamCounts() {
        assertEquals(true, PlayoffBracketSupport.supportsTeamCount(2));
        assertEquals(true, PlayoffBracketSupport.supportsTeamCount(32));
        assertEquals(false, PlayoffBracketSupport.supportsTeamCount(6));
        assertEquals(false, PlayoffBracketSupport.supportsTeamCount(12));
    }

    @Test
    void firstRoundOrdersByPlayoffMatrix() {
        List<BasketDivisionStageTeam> teams = seeds(16, false);
        List<BasketDivisionStageTeam> ordered = PlayoffBracketSupport.orderTeams(teams);
        int[] expected = PlayoffBracketSupport.firstRoundSeedOrder(16);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], ordered.get(i).getSeedNo());
        }
        BasketDivisionStageTeam[] pair = new BasketDivisionStageTeam[2];
        PlayoffBracketSupport.resolveAdjacentPair(ordered, 0, pair);
        assertEquals(1, pair[0].getSeedNo());
        assertEquals(16, pair[1].getSeedNo());
        PlayoffBracketSupport.resolveAdjacentPair(ordered, 1, pair);
        assertEquals(4, pair[0].getSeedNo());
        assertEquals(13, pair[1].getSeedNo());
    }

    @Test
    void inheritedRoundKeepsSeedOrderAndAdjacentPairs() {
        List<BasketDivisionStageTeam> teams = seeds(8, true);
        List<BasketDivisionStageTeam> ordered = PlayoffBracketSupport.orderTeams(teams);
        for (int i = 0; i < 8; i++) {
            assertEquals(i + 1, ordered.get(i).getSeedNo());
        }
        BasketDivisionStageTeam[] pair = new BasketDivisionStageTeam[2];
        PlayoffBracketSupport.resolveAdjacentPair(ordered, 0, pair);
        assertEquals(1, pair[0].getSeedNo());
        assertEquals(2, pair[1].getSeedNo());
        PlayoffBracketSupport.resolveAdjacentPair(ordered, 3, pair);
        assertEquals(7, pair[0].getSeedNo());
        assertEquals(8, pair[1].getSeedNo());
    }

    @Test
    void standingSeedFirstRoundStillUsesPlayoffMatrix() {
        // 来自积分赛晋级：有 sourceStageId，但 entryType=OVERALL_RANK → 仍走首轮象限
        List<BasketDivisionStageTeam> teams = new ArrayList<>();
        for (int i = 16; i >= 1; i--) {
            BasketDivisionStageTeam t = new BasketDivisionStageTeam();
            t.setEventTeamId((long) i);
            t.setSeedNo(i);
            t.setSourceStageId(100L);
            t.setEntryType("OVERALL_RANK");
            teams.add(t);
        }
        List<BasketDivisionStageTeam> ordered = PlayoffBracketSupport.orderTeams(teams);
        int[] expected = PlayoffBracketSupport.firstRoundSeedOrder(16);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], ordered.get(i).getSeedNo());
        }
    }

    @Test
    void useTeamCreateTimeSeedRules() {
        assertEquals(true, PlayoffBracketSupport.useTeamCreateTimeSeed("PLAYOFF_16", true, false));
        assertEquals(false, PlayoffBracketSupport.useTeamCreateTimeSeed("PLAYOFF_8_STD", false, false));
        assertEquals(true, PlayoffBracketSupport.useTeamCreateTimeSeed("PLAYOFF_8_TCT", false, false));
        assertEquals(false, PlayoffBracketSupport.useTeamCreateTimeSeed("PLAYOFF_8_TCT", false, true));
    }

    private static List<BasketDivisionStageTeam> seeds(int n, boolean inherited) {
        List<BasketDivisionStageTeam> teams = new ArrayList<>();
        for (int i = n; i >= 1; i--) {
            BasketDivisionStageTeam t = new BasketDivisionStageTeam();
            t.setEventTeamId((long) i);
            t.setSeedNo(i);
            if (inherited) {
                t.setSourceStageId(100L);
                t.setEntryType("SERIES_WINNER");
            } else {
                t.setEntryType("INITIAL");
            }
            teams.add(t);
        }
        return teams;
    }
}
