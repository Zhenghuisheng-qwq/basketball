package com.zhs.basket.support;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisciplineAnnouncementDraftSupportTest {

    @Test
    void draftTitleUsesTargetAndEventPenalty() {
        String title = DisciplineAnnouncementDraftSupport.buildTitle("S12夜翼", "EVENT_DISQUALIFICATION");
        assertTrue(title.contains("S12夜翼"));
        assertTrue(title.contains("赛事资格处罚"));
        assertTrue(title.startsWith("《"));
        assertTrue(title.endsWith("》"));
    }

    @Test
    void draftBodyDoesNotLeakInternalReason() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.AUGUST, 16, 10, 0, 0);
        Date start = cal.getTime();
        String body = DisciplineAnnouncementDraftSupport.buildContent(
                "S12夜翼", "CNPA Arena", "2026 S12",
                "EVENT_DISQUALIFICATION", start, null);
        assertTrue(body.contains("处罚对象：S12夜翼"));
        assertTrue(body.contains("所属赛事：CNPA Arena 2026 S12"));
        assertTrue(body.contains("处罚类型：取消赛事资格"));
        assertTrue(body.contains("2026-08-16"));
        assertTrue(body.contains("相关比赛结果将依据赛事规则进行处理"));
        assertFalse(body.contains("内部"));
        assertFalse(body.contains("reason"));
        assertFalse(body.contains("公开说明"));
    }

    @Test
    void optionalPublicStatementIsAppendedSeparately() {
        String body = DisciplineAnnouncementDraftSupport.buildContent(
                "S12夜翼", "CNPA Arena", "2026 S12",
                "EVENT_DISQUALIFICATION", new Date(), "违反赛事规则");
        assertTrue(body.contains("公开说明："));
        assertTrue(body.contains("违反赛事规则"));
    }

    @Test
    void staleAnnouncementIdDoesNotDisplayDraft() {
        org.junit.jupiter.api.Assertions.assertEquals("未生成",
                DisciplineAnnouncementDraftSupport.resolveAnnouncementStatusName(null));
        org.junit.jupiter.api.Assertions.assertEquals("草稿",
                DisciplineAnnouncementDraftSupport.resolveAnnouncementStatusName(0));
        org.junit.jupiter.api.Assertions.assertEquals("已发布",
                DisciplineAnnouncementDraftSupport.resolveAnnouncementStatusName(1));
        org.junit.jupiter.api.Assertions.assertEquals("已下线",
                DisciplineAnnouncementDraftSupport.resolveAnnouncementStatusName(2));
        org.junit.jupiter.api.Assertions.assertEquals("未生成",
                DisciplineAnnouncementDraftSupport.resolveAnnouncementStatusName(99));
    }
}
