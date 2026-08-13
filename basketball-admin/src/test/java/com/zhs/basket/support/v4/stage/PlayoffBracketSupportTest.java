package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
