package com.zhs.basket.vo;

import lombok.Data;

@Data
public class ScheduleEventContextVo {
    private Long eventId;
    private Long seasonId;
    private String seasonName;
    private String tournamentName;
    private String platformCode;
    private String platformDisplayName;
    private Integer eventTeamCount;
    private Integer lockedTeamCount;
    private Integer existingMatchCount;
    /** 可安全删除的草稿比赛数 */
    private Integer deletableDraftMatchCount;
    /** 未发布历史脏数据数量（已结束/已取消/已录入等） */
    private Integer legacyUnpublishedMatchCount;
    private Boolean hasLegacyUnpublishedMatches;
    private Boolean hasPublishedSchedule;
    private Boolean hasDraftSchedule;
    private Long latestBatchId;
    private Integer latestBatchPublishStatus;
    private Integer expectedRoundCount;
    private Integer expectedMatchCount;
    private Boolean canGenerateSchedule;
    private String scheduleStatusText;
}
