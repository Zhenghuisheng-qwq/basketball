package com.zhs.basket.support.v4.template;

import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.StageTypeEnum;

final class StandardTemplateStages {
    private StandardTemplateStages() {
    }

    static StandardTemplateStageDefinition roundRobin(String code, String name, int order, String stageType,
                                                      Integer cycles, Integer bestOf, String advancementType,
                                                      Integer advancementCount, String nextStageCode) {
        StandardTemplateStageDefinition stage = base(code, name, order, stageType,
                OrganizationModeEnum.UNGROUPED.getCode(),
                CompetitionFormatEnum.ROUND_ROBIN.getCode(),
                GroupingModeEnum.NONE.getCode());
        stage.setRoundRobinCycles(cycles);
        stage.setBestOf(bestOf);
        stage.setWinPoints(2);
        stage.setLossPoints(1);
        stage.setForfeitWinPoints(2);
        stage.setForfeitLossPoints(0);
        stage.setAdvancementType(advancementType);
        stage.setAdvancementCount(advancementCount);
        stage.setPairingRule(PairingRuleEnum.NONE.getCode());
        stage.setNextStageCode(nextStageCode);
        return stage;
    }

    static StandardTemplateStageDefinition groupedRoundRobin(String code, String name, int order,
                                                             Integer groupCount, Integer cycles, Integer bestOf,
                                                             Integer advancePerGroup, String groupingMode,
                                                             String nextStageCode) {
        StandardTemplateStageDefinition stage = base(code, name, order, StageTypeEnum.GROUP.getCode(),
                OrganizationModeEnum.GROUPED.getCode(),
                CompetitionFormatEnum.ROUND_ROBIN.getCode(),
                groupingMode);
        stage.setGroupCount(groupCount);
        stage.setRoundRobinCycles(cycles);
        stage.setBestOf(bestOf);
        stage.setWinPoints(2);
        stage.setLossPoints(1);
        stage.setForfeitWinPoints(2);
        stage.setForfeitLossPoints(0);
        stage.setAdvancementType(AdvancementTypeEnum.GROUP_TOP_N.getCode());
        stage.setAdvancementCount(advancePerGroup);
        stage.setPairingRule(PairingRuleEnum.NONE.getCode());
        stage.setNextStageCode(nextStageCode);
        return stage;
    }

    static StandardTemplateStageDefinition knockout(String code, String name, int order, Integer bestOf,
                                                    String pairingRule, String seedRule, String drawRule,
                                                    String byeRule) {
        return knockout(code, name, order, bestOf, 5, 5, pairingRule, seedRule, drawRule, byeRule);
    }

    static StandardTemplateStageDefinition knockout(String code, String name, int order, Integer bestOf,
                                                    Integer semifinalBestOf, Integer finalBestOf,
                                                    String pairingRule, String seedRule, String drawRule,
                                                    String byeRule) {
        StandardTemplateStageDefinition stage = base(code, name, order, StageTypeEnum.KNOCKOUT.getCode(),
                OrganizationModeEnum.UNGROUPED.getCode(),
                CompetitionFormatEnum.SINGLE_ELIMINATION.getCode(),
                GroupingModeEnum.NONE.getCode());
        stage.setBestOf(bestOf);
        stage.setSemifinalBestOf(semifinalBestOf);
        stage.setFinalBestOf(finalBestOf);
        stage.setAdvancementType(AdvancementTypeEnum.NONE.getCode());
        stage.setPairingRule(pairingRule);
        stage.setSeedRule(seedRule);
        stage.setDrawRule(drawRule);
        stage.setByeRule(byeRule);
        stage.setRequiresManualConfirmation(
                isManualDraw(drawRule) || isManualBye(byeRule) ? 1 : 0);
        return stage;
    }

    private static boolean isManualDraw(String drawRule) {
        return "MANUAL_SLOT".equals(drawRule);
    }

    private static boolean isManualBye(String byeRule) {
        return "MANUAL".equals(byeRule);
    }

    private static StandardTemplateStageDefinition base(String code, String name, int order, String stageType,
                                                        String organizationMode, String competitionFormat,
                                                        String groupingMode) {
        StandardTemplateStageDefinition stage = new StandardTemplateStageDefinition();
        stage.setStageCode(code);
        stage.setStageName(name);
        stage.setStageOrder(order);
        stage.setStageType(stageType);
        stage.setOrganizationMode(organizationMode);
        stage.setCompetitionFormat(competitionFormat);
        stage.setGroupingMode(groupingMode);
        stage.setRequiresManualConfirmation(0);
        return stage;
    }
}
