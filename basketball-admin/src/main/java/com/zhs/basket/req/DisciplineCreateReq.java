package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class DisciplineCreateReq {
    private String targetType;
    private Long seasonId;
    private Long matchId;
    private Long teamId;
    private Long eventTeamId;
    private Long playerId;
    private Long memberId;
    private String disciplineType;
    private String penaltyCode;
    private String title;
    private String reasonText;
    private String penaltyContent;
    private Integer matchSuspensionCount;
    private Integer pointDeduction;
    private Date effectiveStartTime;
    private Date effectiveEndTime;
    private String idempotencyKey;
}
