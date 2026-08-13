package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketFormatTemplate;
import com.zhs.basket.domain.v4.BasketFormatTemplateStage;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.RosterWorkflowModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DivisionFormatSnapshotFactoryTest {

    private final DivisionFormatSnapshotFactory factory = new DivisionFormatSnapshotFactory();

    @Test
    void buildStages_copiesOrganizationModeFromTemplate() {
        BasketFormatTemplate template = new BasketFormatTemplate();
        template.setId(1L);
        template.setTemplateCode("TEST");
        template.setTemplateName("Test");
        template.setVersionNo(1);

        BasketFormatTemplateStage ts = new BasketFormatTemplateStage();
        ts.setStageCode("S1");
        ts.setStageName("阶段1");
        ts.setStageOrder(1);
        ts.setStageType("GROUP");
        ts.setOrganizationMode(OrganizationModeEnum.GROUPED.getCode());
        ts.setCompetitionFormat("ROUND_ROBIN");
        ts.setGroupingMode(GroupingModeEnum.MANUAL.getCode());
        ts.setGroupCount(4);
        ts.setBestOf(1);
        ts.setAdvancementType("GROUP_TOP_N");
        ts.setAdvancementCount(2);
        ts.setPairingRule("NONE");
        ts.setRequiresManualConfirmation(0);

        List<BasketDivisionStage> stages = factory.buildStages(100L, 200L, List.of(ts));
        assertEquals(1, stages.size());
        assertEquals(OrganizationModeEnum.GROUPED.getCode(), stages.get(0).getOrganizationMode());
        assertEquals("GROUP", stages.get(0).getStageType());
    }

    @Test
    void buildSnapshotHeader_newSnapshotIsVersioned() {
        BasketFormatTemplate template = new BasketFormatTemplate();
        template.setId(1L);
        template.setTemplateCode("TEST");
        template.setTemplateName("Test");
        template.setVersionNo(1);
        var snapshot = factory.buildSnapshotHeader(100L, template, 1, null);
        assertEquals(RosterWorkflowModeEnum.VERSIONED.getCode(), snapshot.getRosterWorkflowMode());
    }
}

