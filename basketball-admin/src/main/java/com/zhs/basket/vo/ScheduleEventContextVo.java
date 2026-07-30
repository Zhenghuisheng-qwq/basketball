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
    private Integer existingMatchCount;
    private Boolean hasPublishedSchedule;
    private Boolean hasDraftSchedule;
    private Long latestBatchId;
    private Integer latestBatchPublishStatus;
}
