package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BestOfCapabilityGateTest {

    @Test
    void lock_allowsBestOf3() {
        assertDoesNotThrow(() -> FormatSnapshotReadySpecification.validateBestOf(3));
        assertDoesNotThrow(() -> FormatSnapshotReadySpecification.validateBestOf(5));
    }

    @Test
    void openRegistration_allowsSingleEliminationBo3() {
        BasketDivisionStage stage = eliminationStage(3);
        assertDoesNotThrow(() -> BestOfCapabilityGate.assertCanOpenRegistration(List.of(stage)));
    }

    @Test
    void openRegistration_blocksRoundRobinBo3() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setStageName("常规赛");
        stage.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
        stage.setBestOf(3);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> BestOfCapabilityGate.assertCanOpenRegistration(List.of(stage)));
        assertTrue(ex.getMessage().contains("循环积分赛"));
    }

    @Test
    void openRegistration_blocksBestOf3WhenUnsupported() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setStageName("未知阶段");
        stage.setCompetitionFormat("UNKNOWN");
        stage.setBestOf(3);
        assertThrows(ServiceException.class, () -> BestOfCapabilityGate.assertCanOpenRegistration(List.of(stage)));
    }

    @Test
    void generateSchedule_allowsSingleEliminationBo5() {
        BasketDivisionStage stage = eliminationStage(5);
        assertDoesNotThrow(() -> BestOfCapabilityGate.assertCanGenerateSchedule(stage));
    }

    @Test
    void generateSchedule_blocksRoundRobinBo3() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
        stage.setBestOf(3);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> BestOfCapabilityGate.assertCanGenerateSchedule(stage));
        assertTrue(ex.getMessage().contains("循环积分"));
    }

    @Test
    void generateSchedule_blocksUnsupportedFormatBo5() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setCompetitionFormat("UNKNOWN");
        stage.setBestOf(5);
        assertThrows(ServiceException.class, () -> BestOfCapabilityGate.assertCanGenerateSchedule(stage));
    }

    @Test
    void bo1_passesGates() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setBestOf(1);
        assertDoesNotThrow(() -> {
            BestOfCapabilityGate.assertCanOpenRegistration(List.of(stage));
            BestOfCapabilityGate.assertCanGenerateSchedule(stage);
        });
    }

    private BasketDivisionStage eliminationStage(int bestOf) {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        stage.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        stage.setGroupingMode(GroupingModeEnum.NONE.getCode());
        stage.setBestOf(bestOf);
        stage.setAdvancementType(AdvancementTypeEnum.WINNER_ADVANCE.getCode());
        return stage;
    }
}
