package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleEliminationR8Test {

    @Test
    void enumReservedExists() {
        assertEquals("DOUBLE_ELIMINATION", CompetitionFormatEnum.DOUBLE_ELIMINATION.getCode());
    }

    @Test
    void generateRejectsDoubleElimination() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setCompetitionFormat(CompetitionFormatEnum.DOUBLE_ELIMINATION.getCode());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> DoubleEliminationCapabilityGate.assertCanGenerateSchedule(stage));
        assertEquals(DoubleEliminationCapabilityGate.RESERVED_MESSAGE, ex.getMessage());
    }

    @Test
    void lockRejectsDoubleEliminationStage() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setCompetitionFormat(CompetitionFormatEnum.DOUBLE_ELIMINATION.getCode());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> DoubleEliminationCapabilityGate.assertCanLockOrPublish(List.of(stage)));
        assertEquals(DoubleEliminationCapabilityGate.RESERVED_MESSAGE, ex.getMessage());
    }

    @Test
    void roundRobinStillAllowed() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
        DoubleEliminationCapabilityGate.assertCanGenerateSchedule(stage);
        assertFalse(DoubleEliminationCapabilityGate.isDoubleElimination(stage));
        assertTrue(DoubleEliminationCapabilityGate.isDoubleElimination(CompetitionFormatEnum.DOUBLE_ELIMINATION.getCode()));
    }
}
