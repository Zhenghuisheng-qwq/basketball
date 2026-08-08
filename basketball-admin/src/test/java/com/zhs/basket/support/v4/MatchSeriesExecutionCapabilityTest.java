package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSeriesExecutionCapabilityTest {

    @Test
    void supportsSingleEliminationBo3() {
        assertTrue(MatchSeriesExecutionCapability.supportsScheduleGeneration(eliminationStage(3)));
    }

    @Test
    void supportsSingleEliminationBo5() {
        assertTrue(MatchSeriesExecutionCapability.supportsScheduleGeneration(eliminationStage(5)));
    }

    @Test
    void rejectsRoundRobinBo3() {
        BasketDivisionStage stage = eliminationStage(3);
        stage.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
        assertFalse(MatchSeriesExecutionCapability.supportsScheduleGeneration(stage));
    }

    @Test
    void rejectsBo1() {
        assertFalse(MatchSeriesExecutionCapability.supportsScheduleGeneration(eliminationStage(1)));
    }

    private BasketDivisionStage eliminationStage(int bestOf) {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        stage.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        stage.setGroupingMode(GroupingModeEnum.NONE.getCode());
        stage.setBestOf(bestOf);
        return stage;
    }
}
