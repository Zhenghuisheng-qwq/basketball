package com.zhs.basket.support.v4;

import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.RosterWorkflowModeEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.enums.v4.StructureStatusEnum;

/**
 * Product-facing Chinese labels. Keep technical codes in DB; UI consumes these labels.
 */
public final class V4AdminDisplaySupport {

    private V4AdminDisplaySupport() {
    }

    public static String ruleStatusLabel(String status) {
        if (SnapshotStatusEnum.DRAFT.getCode().equals(status)) {
            return "草稿";
        }
        if (SnapshotStatusEnum.LOCKED.getCode().equals(status) || SnapshotStatusEnum.IN_USE.getCode().equals(status)) {
            return "已发布 / 已锁定规则";
        }
        if (SnapshotStatusEnum.ARCHIVED.getCode().equals(status)) {
            return "已归档";
        }
        return status == null || status.isEmpty() ? "未配置" : status;
    }

    public static String rosterStatusLabel(String status) {
        if (status == null || status.isEmpty() || "DRAFT".equals(status)) {
            return "名单整理中";
        }
        if ("LOCKED".equals(status)) {
            return "最终名单已锁定";
        }
        return status;
    }

    public static String structureStatusLabel(String status) {
        if (StructureStatusEnum.ACTIVE.getCode().equals(status)) {
            return "当前结构";
        }
        if (StructureStatusEnum.STALE.getCode().equals(status)) {
            return "结构已失效，需重新生成";
        }
        if (status == null || status.isEmpty()) {
            return "尚未生成";
        }
        return status;
    }

    public static String publishStatusLabel(String status) {
        if (PublishStatusEnum.PUBLISHED.getCode().equals(status)) {
            return "已发布";
        }
        if (PublishStatusEnum.WITHDRAWN.getCode().equals(status)) {
            return "已撤回";
        }
        if (PublishStatusEnum.DRAFT.getCode().equals(status) || status == null || status.isEmpty()) {
            return "草稿";
        }
        return status;
    }

    public static String pairingStatusLabel(String status) {
        if (status == null || status.isEmpty() || "NONE".equals(status) || "NOT_REQUIRED".equals(status)) {
            return "不需要";
        }
        if ("CONFIRMED".equals(status)) {
            return "已确认";
        }
        if ("PENDING".equals(status) || "DRAFT".equals(status) || "GENERATED".equals(status)) {
            return "抽签尚未确认";
        }
        return status;
    }

    public static String workflowModeLabel(String mode) {
        if (RosterWorkflowModeEnum.VERSIONED.getCode().equals(mode)) {
            return "版本化";
        }
        if (RosterWorkflowModeEnum.LEGACY.getCode().equals(mode)) {
            return "历史兼容";
        }
        return mode == null ? "-" : mode;
    }

    public static String competitionFormatLabel(String code) {
        if (CompetitionFormatEnum.DOUBLE_ELIMINATION.getCode().equals(code)) {
            return "双败淘汰（预留 / Demo）";
        }
        return V4FormatDisplaySupport.competitionFormatLabel(code);
    }

    public static String placeholderStatusLabel(String resolutionStatus, String sourceType, String sourceDisplay) {
        if ("BYE".equals(sourceType) || "BYE".equals(resolutionStatus)) {
            return "轮空";
        }
        if ("RESOLVED".equals(resolutionStatus)) {
            return sourceDisplay == null || sourceDisplay.isEmpty() ? "已确定" : "已确定：" + sourceDisplay;
        }
        if (ParticipantSourceLabels.isGroupRank(sourceType)) {
            return sourceDisplay == null || sourceDisplay.isEmpty() ? "待小组赛结果确定" : "待小组赛结果确定（" + sourceDisplay + "）";
        }
        if (ParticipantSourceLabels.isWinnerOfSeries(sourceType)) {
            return sourceDisplay == null || sourceDisplay.isEmpty() ? "等待系列赛胜者" : sourceDisplay;
        }
        if ("PENDING".equals(resolutionStatus) || "TBD".equals(resolutionStatus) || "UNRESOLVED".equals(resolutionStatus)) {
            return sourceDisplay == null || sourceDisplay.isEmpty() ? "待确定" : sourceDisplay;
        }
        return sourceDisplay == null || sourceDisplay.isEmpty() ? "-" : sourceDisplay;
    }

    private static final class ParticipantSourceLabels {
        private ParticipantSourceLabels() {
        }

        static boolean isGroupRank(String sourceType) {
            return "GROUP_RANK".equals(sourceType);
        }

        static boolean isWinnerOfSeries(String sourceType) {
            return "WINNER_OF_SERIES".equals(sourceType);
        }
    }
}
