package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class TeamRosterLockResultVo {
    private Long rosterVersionId;
    private Integer versionNo;
    private String status;
    private Integer formalCount;
    private Integer candidateCount;
    private Integer excludedCount;
    private Boolean idempotentReplay;
}
