package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import com.zhs.basket.support.v4.stage.RoundRobinStageScheduleStrategy;
import com.zhs.basket.support.v4.stage.SingleEliminationStageScheduleStrategy;
import com.zhs.basket.support.v4.stage.GroupStageStageScheduleStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerSupportsR12Test {

    private final RoundRobinStageScheduleStrategy roundRobin = new RoundRobinStageScheduleStrategy();
    private final GroupStageStageScheduleStrategy groupStage = new GroupStageStageScheduleStrategy();
    private final SingleEliminationStageScheduleStrategy elimination = new SingleEliminationStageScheduleStrategy();

    @Test
    void fourTeamRoundRobin_ungrouped() {
        BasketDivisionStage stage = rrStage(OrganizationModeEnum.UNGROUPED);
        assertTrue(roundRobin.supports(stage));
        assertFalse(groupStage.supports(stage));
        assertFalse(elimination.supports(stage));
    }

    @Test
    void fiveTeamBye_ungrouped() {
        BasketDivisionStage stage = rrStage(OrganizationModeEnum.UNGROUPED);
        assertTrue(roundRobin.supports(stage));
    }

    @Test
    void groupBo1_grouped() {
        BasketDivisionStage stage = rrStage(OrganizationModeEnum.GROUPED);
        stage.setGroupingMode(GroupingModeEnum.MANUAL.getCode());
        stage.setGroupCount(4);
        assertTrue(groupStage.supports(stage));
        assertFalse(roundRobin.supports(stage));
    }

    @Test
    void eliminationBo1_ungrouped() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        stage.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        stage.setGroupingMode(GroupingModeEnum.NONE.getCode());
        stage.setBestOf(1);
        assertTrue(elimination.supports(stage));
        assertFalse(roundRobin.supports(stage));
        assertFalse(groupStage.supports(stage));
    }

    private BasketDivisionStage rrStage(OrganizationModeEnum orgMode) {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setOrganizationMode(orgMode.getCode());
        stage.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
        stage.setGroupingMode(orgMode == OrganizationModeEnum.GROUPED
                ? GroupingModeEnum.MANUAL.getCode() : GroupingModeEnum.NONE.getCode());
        stage.setBestOf(1);
        return stage;
    }
}
