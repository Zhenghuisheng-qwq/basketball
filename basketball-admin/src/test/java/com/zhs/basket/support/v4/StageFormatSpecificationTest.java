package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StageFormatSpecificationTest {

    @Test
    void pass_ungroupedRoundRobin() {
        BasketDivisionStage stage = baseStage();
        stage.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        stage.setGroupingMode(GroupingModeEnum.NONE.getCode());
        stage.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
        assertDoesNotThrow(() -> {
            StageFormatSpecification.validateOrganizationAndGrouping(stage);
            StageFormatSpecification.validateAllowedCombination(stage);
        });
    }

    @Test
    void pass_groupedRoundRobin() {
        BasketDivisionStage stage = baseStage();
        stage.setOrganizationMode(OrganizationModeEnum.GROUPED.getCode());
        stage.setGroupingMode(GroupingModeEnum.MANUAL.getCode());
        stage.setGroupCount(4);
        stage.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
        assertDoesNotThrow(() -> {
            StageFormatSpecification.validateOrganizationAndGrouping(stage);
            StageFormatSpecification.validateAllowedCombination(stage);
        });
    }

    @Test
    void pass_ungroupedElimination() {
        BasketDivisionStage stage = baseStage();
        stage.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        stage.setGroupingMode(GroupingModeEnum.NONE.getCode());
        stage.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        assertDoesNotThrow(() -> StageFormatSpecification.validateAllowedCombination(stage));
    }

    @Test
    void fail_ungroupedWithManualGrouping() {
        BasketDivisionStage stage = baseStage();
        stage.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        stage.setGroupingMode(GroupingModeEnum.MANUAL.getCode());
        assertThrows(ServiceException.class, () -> StageFormatSpecification.validateOrganizationAndGrouping(stage));
    }

    @Test
    void fail_ungroupedWithGroupCount() {
        BasketDivisionStage stage = baseStage();
        stage.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        stage.setGroupingMode(GroupingModeEnum.NONE.getCode());
        stage.setGroupCount(4);
        assertThrows(ServiceException.class, () -> StageFormatSpecification.validateOrganizationAndGrouping(stage));
    }

    @Test
    void fail_groupedWithNoneGrouping() {
        BasketDivisionStage stage = baseStage();
        stage.setOrganizationMode(OrganizationModeEnum.GROUPED.getCode());
        stage.setGroupingMode(GroupingModeEnum.NONE.getCode());
        assertThrows(ServiceException.class, () -> StageFormatSpecification.validateOrganizationAndGrouping(stage));
    }

    @Test
    void fail_groupedWithZeroGroupCount() {
        BasketDivisionStage stage = baseStage();
        stage.setOrganizationMode(OrganizationModeEnum.GROUPED.getCode());
        stage.setGroupingMode(GroupingModeEnum.MANUAL.getCode());
        stage.setGroupCount(0);
        assertThrows(ServiceException.class, () -> StageFormatSpecification.validateOrganizationAndGrouping(stage));
    }

    @Test
    void fail_groupedElimination() {
        BasketDivisionStage stage = baseStage();
        stage.setOrganizationMode(OrganizationModeEnum.GROUPED.getCode());
        stage.setGroupingMode(GroupingModeEnum.MANUAL.getCode());
        stage.setGroupCount(4);
        stage.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        assertThrows(ServiceException.class, () -> StageFormatSpecification.validateAllowedCombination(stage));
    }

    @Test
    void fail_missingOrganizationModeOnWrite() {
        assertThrows(ServiceException.class, () -> StageFormatSpecification.requireExplicitOrganizationMode(null));
        assertThrows(ServiceException.class, () -> StageFormatSpecification.requireExplicitOrganizationMode(""));
    }

    @Test
    void winnerAdvance_nonFinalElimination_pass() {
        BasketDivisionStage stage = baseStage();
        stage.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        stage.setAdvancementType(AdvancementTypeEnum.WINNER_ADVANCE.getCode());
        assertDoesNotThrow(() -> StageFormatSpecification.validateEliminationRules(stage, false));
    }

    @Test
    void winnerAdvance_finalElimination_fail() {
        BasketDivisionStage stage = baseStage();
        stage.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        stage.setAdvancementType(AdvancementTypeEnum.WINNER_ADVANCE.getCode());
        assertThrows(ServiceException.class, () -> StageFormatSpecification.validateEliminationRules(stage, true));
    }

    @Test
    void none_finalElimination_pass() {
        BasketDivisionStage stage = baseStage();
        stage.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        stage.setAdvancementType(AdvancementTypeEnum.NONE.getCode());
        assertDoesNotThrow(() -> StageFormatSpecification.validateEliminationRules(stage, true));
    }

    @Test
    void none_nonFinalElimination_fail() {
        BasketDivisionStage stage = baseStage();
        stage.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        stage.setAdvancementType(AdvancementTypeEnum.NONE.getCode());
        assertThrows(ServiceException.class, () -> StageFormatSpecification.validateEliminationRules(stage, false));
    }

    private BasketDivisionStage baseStage() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setStageName("测试阶段");
        stage.setRoundRobinCycles(1);
        stage.setWinPoints(2);
        stage.setLossPoints(1);
        return stage;
    }
}
