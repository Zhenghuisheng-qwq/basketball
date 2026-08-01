package com.zhs.basket.vo;

import lombok.Data;

@Data
public class ScheduleGeneratePreviewVo {
    private Long eventId;
    private String seasonName;
    private String tournamentName;
    private String platformCode;
    private Integer eventTeamCount;
    private Integer lockedTeamCount;
    private Integer expectedMatchCount;
    private Integer expectedRoundCount;
    private String ruleName;
    private String ruleCode;
    private String ruleVersion;
}
