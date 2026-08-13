package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class StagePairingEntryVo {
    private Integer slotNo;
    private Long eventTeamId;
    private String teamName;
    private Integer seedNo;
    private Integer rankingNo;
    private Boolean bye;
    private String sourceType;
    private Long sourceStageId;
    private Long sourceGroupId;
    private String sourceGroupCode;
    private Integer sourceRank;
    private String displayCode;
    private String displayName;
    private String resolutionStatus;
    private Long resolvedEventTeamId;
    private String resolvedEventTeamName;
}
