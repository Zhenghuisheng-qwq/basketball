package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.enums.v4.DivisionStageStatusEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.enums.v4.StageExecutionStatusEnum;
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
        snapshot.setSnapshotVersion(snapshotVersion);
        snapshot.setSnapshotStatus(SnapshotStatusEnum.DRAFT.getCode());
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
            stage.setWinPoints(ts.getWinPoints());
            stage.setLossPoints(ts.getLossPoints());
            stage.setForfeitWinPoints(ts.getForfeitWinPoints());
            stage.setForfeitLossPoints(ts.getForfeitLossPoints());
            stage.setAdvancementType(ts.getAdvancementType());
            stage.setAdvancementCount(ts.getAdvancementCount());
            stage.setPairingRule(ts.getPairingRule());
            stage.setNextStageCode(ts.getNextStageCode());
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
}
