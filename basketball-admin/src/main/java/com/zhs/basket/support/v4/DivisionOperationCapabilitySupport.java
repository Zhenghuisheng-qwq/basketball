package com.zhs.basket.support.v4;

import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.RosterWorkflowModeEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.vo.v4.DivisionOperationCapabilities;
import com.zhs.basket.vo.v4.StageOperationCapabilities;

/**
 * Capability flags for R8 UI. Calls existing status semantics, does not duplicate Guards.
 */
public final class DivisionOperationCapabilitySupport {

    public static final String MSG_PUBLISH_RULE_FIRST = "请先完成赛事规则发布";
    public static final String MSG_LOCK_ROSTER_FIRST = "请先完成并锁定最终参赛名单";
    public static final String MSG_STRUCTURE_STALE = "当前赛程结构已因参赛名单变化失效，请重新生成。";
    public static final String MSG_STRUCTURE_MISSING = "请先生成赛程结构";
    public static final String MSG_SCHEDULE_INCOMPLETE_PREFIX = "还有 ";
    public static final String MSG_SCHEDULE_INCOMPLETE_SUFFIX = " 场未排期";
    public static final String MSG_PUBLISHED_READONLY = "赛段已发布，请先撤回后再修改";
    public static final String MSG_DOUBLE_ELIM = DoubleEliminationCapabilityGate.RESERVED_MESSAGE;

    private DivisionOperationCapabilitySupport() {
    }

    public static boolean isRuleLocked(String ruleStatus) {
        if (ruleStatus == null || ruleStatus.isEmpty()) {
            return false;
        }
        try {
            return SnapshotStatusEnum.fromCode(ruleStatus).isLockedOrBeyond();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean isVersioned(String rosterWorkflowMode) {
        return RosterWorkflowModeEnum.VERSIONED.getCode().equals(rosterWorkflowMode);
    }

    public static boolean isRosterLocked(String rosterStatus) {
        return "LOCKED".equals(rosterStatus);
    }

    public static DivisionOperationCapabilities divisionCapabilities(
            String rosterWorkflowMode,
            String ruleStatus,
            String rosterStatus,
            boolean ruleMutable,
            boolean hasDoubleElimination) {
        DivisionOperationCapabilities cap = new DivisionOperationCapabilities();
        boolean versioned = isVersioned(rosterWorkflowMode);
        boolean ruleLocked = isRuleLocked(ruleStatus);
        boolean rosterLocked = isRosterLocked(rosterStatus);
        cap.setLegacyCompatible(!versioned);
        cap.setRequireRulePublished(versioned);
        cap.setCanEditRule(ruleMutable && !hasDoubleElimination);
        cap.setCanLockRule(!ruleLocked && !hasDoubleElimination);
        cap.setCanManageRoster(ruleLocked && !rosterLocked);
        cap.setCanLockRoster(ruleLocked && !rosterLocked);
        cap.setCanGenerateStructure(ruleLocked && (!versioned || rosterLocked) && !hasDoubleElimination);
        cap.setCanEditSchedule(false);
        cap.setCanPublishStage(false);
        cap.setCanWithdrawStage(false);
        if (hasDoubleElimination) {
            cap.setBlockReason(MSG_DOUBLE_ELIM);
        } else if (versioned && !ruleLocked) {
            cap.setBlockReason(MSG_PUBLISH_RULE_FIRST);
        } else if (versioned && !rosterLocked) {
            cap.setBlockReason(MSG_LOCK_ROSTER_FIRST);
        }
        return cap;
    }

    public static StageOperationCapabilities stageCapabilities(
            boolean ruleLocked,
            boolean rosterReady,
            String structureStatus,
            String publishStatus,
            Integer unscheduledUnits,
            boolean canRecommend,
            boolean pairingConfirmedOrNotRequired,
            boolean qualificationReady,
            boolean doubleElimination) {
        StageOperationCapabilities cap = new StageOperationCapabilities();
        boolean published = PublishStatusEnum.PUBLISHED.getCode().equals(publishStatus);
        boolean stale = StructureStatusEnum.STALE.getCode().equals(structureStatus);
        boolean hasStructure = structureStatus != null && !structureStatus.isEmpty();
        boolean active = StructureStatusEnum.ACTIVE.getCode().equals(structureStatus);
        int unscheduled = unscheduledUnits == null ? 0 : unscheduledUnits;

        if (doubleElimination) {
            cap.setBlockReason(MSG_DOUBLE_ELIM);
            return cap;
        }
        if (!ruleLocked) {
            cap.setBlockReason(MSG_PUBLISH_RULE_FIRST);
            return cap;
        }
        if (!rosterReady) {
            cap.setBlockReason(MSG_LOCK_ROSTER_FIRST);
            return cap;
        }
        if (!qualificationReady) {
            cap.setBlockReason("等待小组赛排名确认");
            return cap;
        }
        cap.setCanGenerateStructure(!published && pairingConfirmedOrNotRequired);
        if (stale) {
            cap.setBlockReason(MSG_STRUCTURE_STALE);
            cap.setCanGenerateStructure(true);
            return cap;
        }
        if (!hasStructure) {
            cap.setBlockReason(MSG_STRUCTURE_MISSING);
            return cap;
        }
        if (published) {
            cap.setCanWithdraw(true);
            cap.setBlockReason(MSG_PUBLISHED_READONLY);
            return cap;
        }
        if (!pairingConfirmedOrNotRequired) {
            cap.setBlockReason("抽签尚未确认");
            return cap;
        }
        cap.setCanEditSchedule(active);
        cap.setCanRecommendSchedule(active && canRecommend);
        if (unscheduled > 0) {
            cap.setBlockReason(MSG_SCHEDULE_INCOMPLETE_PREFIX + unscheduled + MSG_SCHEDULE_INCOMPLETE_SUFFIX);
            return cap;
        }
        cap.setCanPublish(active && unscheduled == 0);
        return cap;
    }

    public static String selectorBlockReason(String entry, String rosterWorkflowMode, String ruleStatus) {
        if (entry == null || "publish".equals(entry) || "01".equals(entry)) {
            return null;
        }
        if (isVersioned(rosterWorkflowMode) && !isRuleLocked(ruleStatus)) {
            return MSG_PUBLISH_RULE_FIRST;
        }
        return null;
    }
}
