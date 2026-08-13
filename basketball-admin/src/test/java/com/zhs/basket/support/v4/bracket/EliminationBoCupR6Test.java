package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.support.v4.BestOfValues;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EliminationBoCupR6Test {

    @Test
    void eightQualifiersUseQfBo3SfBo5FinalBo5() {
        BasketDivisionStage stage = stage(3, 5, 5);
        assertEquals(3, EliminationBoMapping.bestOf(stage, 1, 3));
        assertEquals(5, EliminationBoMapping.bestOf(stage, 2, 3));
        assertEquals(5, EliminationBoMapping.bestOf(stage, 3, 3));
        assertEquals(EliminationBoRoundRole.EARLY, EliminationBoMapping.roleOf(1, 3));
        assertEquals(EliminationBoRoundRole.SEMIFINAL, EliminationBoMapping.roleOf(2, 3));
        assertEquals(EliminationBoRoundRole.FINAL, EliminationBoMapping.roleOf(3, 3));
    }

    @Test
    void finalBo7RequiresFourWinsAndSevenGames() {
        BestOfValues.requireLegal(7);
        assertEquals(4, BestOfValues.requiredWins(7));
        BasketDivisionStage stage = stage(3, 5, 7);
        assertEquals(7, EliminationBoMapping.bestOf(stage, 3, 3));
    }

    private static BasketDivisionStage stage(int early, int sf, int fi) {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setBestOf(early);
        stage.setSemifinalBestOf(sf);
        stage.setFinalBestOf(fi);
        return stage;
    }
}
