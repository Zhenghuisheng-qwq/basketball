package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketFormatTemplateStage;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationModeSupportTest {

    @Test
    void backfill_noneGroupingMode_resolvesUngrouped() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setGroupingMode(GroupingModeEnum.NONE.getCode());
        assertEquals(OrganizationModeEnum.UNGROUPED.getCode(),
                OrganizationModeSupport.resolveOrganizationMode(stage));
    }

    @Test
    void backfill_manualGroupingMode_resolvesGrouped() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setGroupingMode(GroupingModeEnum.MANUAL.getCode());
        assertEquals(OrganizationModeEnum.GROUPED.getCode(),
                OrganizationModeSupport.resolveOrganizationMode(stage));
    }

    @Test
    void explicitOrganizationMode_takesPrecedence() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setOrganizationMode(OrganizationModeEnum.GROUPED.getCode());
        stage.setGroupingMode(GroupingModeEnum.NONE.getCode());
        assertEquals(OrganizationModeEnum.GROUPED.getCode(),
                OrganizationModeSupport.resolveOrganizationMode(stage));
    }

    @Test
    void resolveForPersist_prefersExplicitMode() {
        assertEquals(OrganizationModeEnum.GROUPED.getCode(),
                OrganizationModeSupport.resolveForPersist(
                        OrganizationModeEnum.GROUPED.getCode(), GroupingModeEnum.NONE.getCode()));
    }

    @Test
    void resolveForPersist_derivesFromGroupingWhenMissing_legacyMigrationOnly() {
        assertEquals(OrganizationModeEnum.UNGROUPED.getCode(),
                OrganizationModeSupport.resolveForPersist(null, GroupingModeEnum.NONE.getCode()));
        assertEquals(OrganizationModeEnum.GROUPED.getCode(),
                OrganizationModeSupport.resolveForPersist(null, GroupingModeEnum.MANUAL.getCode()));
    }

    @Test
    void requireExplicitForWrite_rejectsNull() {
        assertThrows(com.zhs.common.exception.ServiceException.class,
                () -> StageFormatSpecification.requireExplicitOrganizationMode(null));
    }

    @Test
    void templateStage_fallbackGrouped() {
        BasketFormatTemplateStage stage = new BasketFormatTemplateStage();
        stage.setGroupingMode(GroupingModeEnum.SEEDED.getCode());
        assertEquals(OrganizationModeEnum.GROUPED.getCode(),
                OrganizationModeSupport.resolveOrganizationMode(stage));
    }
}
