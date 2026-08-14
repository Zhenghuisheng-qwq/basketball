package com.zhs.basket.support.v4;

import com.zhs.basket.vo.v4.DivisionOperationCapabilities;
import com.zhs.basket.vo.v4.StageOperationCapabilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DivisionOperationCapabilityR8Test {

    @Test
    void versionedWithoutRuleBlocksLaterEntries() {
        DivisionOperationCapabilities caps = DivisionOperationCapabilitySupport.divisionCapabilities(
                "VERSIONED", "DRAFT", "DRAFT", true, false);
        assertEquals(DivisionOperationCapabilitySupport.MSG_PUBLISH_RULE_FIRST, caps.getBlockReason());
        assertTrue(caps.isRequireRulePublished());
        assertEquals(DivisionOperationCapabilitySupport.MSG_PUBLISH_RULE_FIRST,
                DivisionOperationCapabilitySupport.selectorBlockReason("structure", "VERSIONED", "DRAFT"));
        assertEquals(null, DivisionOperationCapabilitySupport.selectorBlockReason("publish", "VERSIONED", "DRAFT"));
    }

    @Test
    void lockedRuleAllowsRosterAndLegacyCompatible() {
        DivisionOperationCapabilities caps = DivisionOperationCapabilitySupport.divisionCapabilities(
                "VERSIONED", "LOCKED", "DRAFT", false, false);
        assertTrue(caps.isCanManageRoster());
        assertTrue(caps.isCanLockRoster());
        assertFalse(caps.isCanEditRule());
        assertEquals(DivisionOperationCapabilitySupport.MSG_LOCK_ROSTER_FIRST, caps.getBlockReason());
    }

    @Test
    void staleStructureBlocksScheduleAndPublish() {
        StageOperationCapabilities cap = DivisionOperationCapabilitySupport.stageCapabilities(
                true, true, "STALE", "DRAFT", 0, true, true, true, false);
        assertTrue(cap.isCanGenerateStructure());
        assertFalse(cap.isCanEditSchedule());
        assertFalse(cap.isCanPublish());
        assertEquals(DivisionOperationCapabilitySupport.MSG_STRUCTURE_STALE, cap.getBlockReason());
    }

    @Test
    void publishedStageIsReadOnlyAndCanWithdraw() {
        StageOperationCapabilities cap = DivisionOperationCapabilitySupport.stageCapabilities(
                true, true, "ACTIVE", "PUBLISHED", 0, false, true, true, false);
        assertTrue(cap.isCanWithdraw());
        assertFalse(cap.isCanEditSchedule());
        assertEquals(DivisionOperationCapabilitySupport.MSG_PUBLISHED_READONLY, cap.getBlockReason());
    }

    @Test
    void displayLabelsAreProductLanguage() {
        assertEquals("已发布 / 已锁定规则", V4AdminDisplaySupport.ruleStatusLabel("LOCKED"));
        assertEquals("草稿", V4AdminDisplaySupport.ruleStatusLabel("DRAFT"));
        assertEquals("名单整理中", V4AdminDisplaySupport.rosterStatusLabel("DRAFT"));
        assertEquals("最终名单已锁定", V4AdminDisplaySupport.rosterStatusLabel("LOCKED"));
        assertEquals("当前结构", V4AdminDisplaySupport.structureStatusLabel("ACTIVE"));
        assertEquals("结构已失效，需重新生成", V4AdminDisplaySupport.structureStatusLabel("STALE"));
        assertEquals("已发布", V4AdminDisplaySupport.publishStatusLabel("PUBLISHED"));
        assertEquals("已撤回", V4AdminDisplaySupport.publishStatusLabel("WITHDRAWN"));
        assertEquals("待小组赛结果确定", V4AdminDisplaySupport.placeholderStatusLabel("PENDING", "GROUP_RANK", null));
        assertEquals("QF1 胜者", V4AdminDisplaySupport.placeholderStatusLabel("PENDING", "WINNER_OF_SERIES", "QF1 胜者"));
        assertEquals("轮空", V4AdminDisplaySupport.placeholderStatusLabel(null, "BYE", null));
    }
}
