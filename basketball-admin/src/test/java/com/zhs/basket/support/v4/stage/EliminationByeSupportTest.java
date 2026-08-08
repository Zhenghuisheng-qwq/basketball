package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EliminationByeSupportTest {

    @Test
    void sevenTeams_allowsBye_forNonFinalSingleRound() {
        BasketDivisionStage stage = nonFinalStage();
        assertTrue(EliminationByeSupport.allowsBye(stage, 7));
        assertEquals(3, EliminationByeSupport.pairingSlotCount(7));
    }

    @Test
    void sevenTeams_highestSeedGetsBye() {
        List<BasketDivisionStageTeam> teams = teamsWithSeeds(7);
        List<BasketDivisionStageTeam> sorted = EliminationByeSupport.sortTeams(teams);
        Long byeId = EliminationByeSupport.resolveByeEventTeamId(sorted);
        assertEquals(7L, byeId);
        assertEquals(6, EliminationByeSupport.pairingTeams(sorted, byeId).size());
    }

    @Test
    void sixTeams_noBye() {
        BasketDivisionStage stage = nonFinalStage();
        assertFalse(EliminationByeSupport.allowsBye(stage, 6));
        assertEquals(3, EliminationByeSupport.pairingSlotCount(6));
    }

    @Test
    void finalStage_noByeEvenIfOdd() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setAdvancementType("NONE");
        stage.setNextStageCode(null);
        assertFalse(EliminationByeSupport.allowsBye(stage, 7));
    }

    private static BasketDivisionStage nonFinalStage() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setAdvancementType("WINNER_ADVANCE");
        stage.setNextStageCode("SF");
        stage.setCompetitionFormat("SINGLE_ELIMINATION");
        stage.setBestOf(1);
        return stage;
    }

    private static List<BasketDivisionStageTeam> teamsWithSeeds(int count) {
        List<BasketDivisionStageTeam> teams = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BasketDivisionStageTeam t = new BasketDivisionStageTeam();
            t.setEventTeamId((long) i);
            t.setSeedNo(count - i + 1);
            teams.add(t);
        }
        return teams;
    }
}
