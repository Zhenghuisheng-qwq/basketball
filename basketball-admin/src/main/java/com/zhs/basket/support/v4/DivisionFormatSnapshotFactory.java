package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.DivisionStageStatusEnum;
import com.zhs.basket.enums.v4.RosterWorkflowModeEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.enums.v4.StageExecutionStatusEnum;
import com.zhs.basket.enums.v4.TemplateTypeEnum;
import com.zhs.basket.support.v4.template.StandardTemplateCodes;
import com.zhs.basket.support.v4.template.StandardTemplateDefinition;
import com.zhs.basket.support.v4.template.StandardTemplateStageDefinition;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 规则快照工厂：将模板复制为 Division 快照聚合。
 */
@Component
public class DivisionFormatSnapshotFactory {

    public static String buildActiveUniqueKey(Long divisionId) {
        return "DIVISION:" + divisionId;
    }

    public BasketDivisionFormatSnapshot buildSnapshotHeader(Long divisionId, BasketFormatTemplate template,
                                                            int snapshotVersion, Long supersededSnapshotId) {
        Date now = new Date();
        BasketDivisionFormatSnapshot snapshot = new BasketDivisionFormatSnapshot();
        snapshot.setDivisionId(divisionId);
        snapshot.setSourceTemplateId(template.getId());
        snapshot.setSourceTemplateCode(template.getTemplateCode());
        snapshot.setSourceTemplateName(template.getTemplateName());
        snapshot.setSourceTemplateVersion(template.getVersionNo());
        snapshot.setSourceTemplateType(resolveTemplateType(template));
        snapshot.setSnapshotVersion(snapshotVersion);
        snapshot.setSnapshotStatus(SnapshotStatusEnum.DRAFT.getCode());
        snapshot.setRosterWorkflowMode(RosterWorkflowModeEnum.VERSIONED.getCode());
        snapshot.setIsCurrent(1);
        snapshot.setActiveUniqueKey(buildActiveUniqueKey(divisionId));
        snapshot.setSupersededSnapshotId(supersededSnapshotId);
        snapshot.setCreateTime(now);
        snapshot.setUpdateTime(now);
        snapshot.setIsDelete(0);
        return snapshot;
    }

    public List<BasketDivisionStage> buildStages(Long divisionId, Long snapshotId,
                                                  List<BasketFormatTemplateStage> templateStages) {
        Date now = new Date();
        List<BasketDivisionStage> stages = new ArrayList<>();
        for (BasketFormatTemplateStage ts : templateStages) {
            BasketDivisionStage stage = new BasketDivisionStage();
            stage.setSnapshotId(snapshotId);
            stage.setDivisionId(divisionId);
            stage.setStageCode(ts.getStageCode());
            stage.setStageName(ts.getStageName());
            stage.setStageOrder(ts.getStageOrder());
            stage.setStageType(ts.getStageType());
            stage.setOrganizationMode(ts.getOrganizationMode());
            stage.setCompetitionFormat(ts.getCompetitionFormat());
            stage.setGroupingMode(ts.getGroupingMode());
            stage.setGroupCount(ts.getGroupCount());
            stage.setRoundRobinCycles(ts.getRoundRobinCycles());
            stage.setBestOf(ts.getBestOf());
        stage.setSemifinalBestOf(ts.getSemifinalBestOf());
        stage.setFinalBestOf(ts.getFinalBestOf());
            stage.setWinPoints(ts.getWinPoints());
            stage.setLossPoints(ts.getLossPoints());
            stage.setForfeitWinPoints(ts.getForfeitWinPoints());
            stage.setForfeitLossPoints(ts.getForfeitLossPoints());
            stage.setAdvancementType(ts.getAdvancementType());
            stage.setAdvancementCount(ts.getAdvancementCount());
            stage.setPairingRule(ts.getPairingRule());
            stage.setNextStageCode(ts.getNextStageCode());
            stage.setSeedRule(ts.getSeedRule());
            stage.setDrawRule(ts.getDrawRule());
            stage.setByeRule(ts.getByeRule());
            stage.setRequiresManualConfirmation(ts.getRequiresManualConfirmation());
            stage.setStageStatus(DivisionStageStatusEnum.DRAFT.getCode());
            stage.setExecutionStatus(StageExecutionStatusEnum.DRAFT.getCode());
            stage.setExecutionVersion(0);
            stage.setCreateTime(now);
            stage.setUpdateTime(now);
            stage.setIsDelete(0);
            stages.add(stage);
        }
        return stages;
    }

    public List<BasketDivisionTieBreakRule> buildTieBreakRules(Long divisionId, Long snapshotId,
                                                                List<BasketFormatTemplateTieBreakRule> templateRules,
                                                                Map<String, Long> stageCodeToId) {
        Date now = new Date();
        List<BasketDivisionTieBreakRule> rules = new ArrayList<>();
        for (BasketFormatTemplateTieBreakRule tr : templateRules) {
            Long stageId = stageCodeToId.get(tr.getStageCode());
            if (stageId == null) {
                continue;
            }
            BasketDivisionTieBreakRule rule = new BasketDivisionTieBreakRule();
            rule.setSnapshotId(snapshotId);
            rule.setDivisionId(divisionId);
            rule.setStageId(stageId);
            rule.setRuleCode(tr.getRuleCode());
            rule.setPriorityNo(tr.getPriorityNo());
            rule.setEnabled(tr.getEnabled());
            rule.setCreateTime(now);
            rule.setUpdateTime(now);
            rule.setIsDelete(0);
            rules.add(rule);
        }
        return rules;
    }

    public List<BasketDivisionStage> buildStagesFromDefinition(Long divisionId, Long snapshotId,
                                                               StandardTemplateDefinition definition) {
        Date now = new Date();
        List<BasketDivisionStage> stages = new ArrayList<>();
        if (definition == null || definition.getStages() == null) {
            return stages;
        }
        for (StandardTemplateStageDefinition ts : definition.getStages()) {
            BasketDivisionStage stage = new BasketDivisionStage();
            stage.setSnapshotId(snapshotId);
            stage.setDivisionId(divisionId);
            copyStageDefinition(stage, ts);
            stage.setStageStatus(DivisionStageStatusEnum.DRAFT.getCode());
            stage.setExecutionStatus(StageExecutionStatusEnum.DRAFT.getCode());
            stage.setExecutionVersion(0);
            stage.setCreateTime(now);
            stage.setUpdateTime(now);
            stage.setIsDelete(0);
            stages.add(stage);
        }
        return stages;
    }

    public void applyDefinition(List<BasketDivisionStage> stages, StandardTemplateDefinition definition) {
        if (stages == null || definition == null || definition.getStages() == null) {
            return;
        }
        Map<String, StandardTemplateStageDefinition> byCode = new HashMap<>();
        for (StandardTemplateStageDefinition ts : definition.getStages()) {
            byCode.put(ts.getStageCode(), ts);
        }
        for (BasketDivisionStage stage : stages) {
            StandardTemplateStageDefinition ts = byCode.get(stage.getStageCode());
            if (ts != null) {
                copyStageDefinition(stage, ts);
            }
        }
    }

    private void copyStageDefinition(BasketDivisionStage stage, StandardTemplateStageDefinition ts) {
        stage.setStageCode(ts.getStageCode());
        stage.setStageName(ts.getStageName());
        stage.setStageOrder(ts.getStageOrder());
        stage.setStageType(ts.getStageType());
        stage.setOrganizationMode(ts.getOrganizationMode());
        stage.setCompetitionFormat(ts.getCompetitionFormat());
        stage.setGroupingMode(ts.getGroupingMode());
        stage.setGroupCount(ts.getGroupCount());
        stage.setRoundRobinCycles(ts.getRoundRobinCycles());
        stage.setBestOf(ts.getBestOf());
        stage.setSemifinalBestOf(ts.getSemifinalBestOf());
        stage.setFinalBestOf(ts.getFinalBestOf());
        stage.setWinPoints(ts.getWinPoints());
        stage.setLossPoints(ts.getLossPoints());
        stage.setForfeitWinPoints(ts.getForfeitWinPoints());
        stage.setForfeitLossPoints(ts.getForfeitLossPoints());
        stage.setAdvancementType(ts.getAdvancementType());
        stage.setAdvancementCount(ts.getAdvancementCount());
        stage.setPairingRule(ts.getPairingRule());
        stage.setNextStageCode(ts.getNextStageCode());
        stage.setSeedRule(ts.getSeedRule());
        stage.setDrawRule(ts.getDrawRule());
        stage.setByeRule(ts.getByeRule());
        stage.setRequiresManualConfirmation(ts.getRequiresManualConfirmation() == null ? 0 : ts.getRequiresManualConfirmation());
    }

    private static String resolveTemplateType(BasketFormatTemplate template) {
        if (template.getTemplateType() != null && !template.getTemplateType().isEmpty()) {
            return template.getTemplateType();
        }
        if (StandardTemplateCodes.isStandard(template.getTemplateCode())) {
            return TemplateTypeEnum.STANDARD.getCode();
        }
        return TemplateTypeEnum.CUSTOM.getCode();
    }
}
