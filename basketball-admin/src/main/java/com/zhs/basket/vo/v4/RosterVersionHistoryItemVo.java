package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class RosterVersionHistoryItemVo {
    private Long rosterVersionId;
    private Integer versionNo;
    private String status;
    private String statusName;
    private Integer isCurrent;
    private Integer formalCount;
    private Integer candidateCount;
    private Integer excludedCount;
    private Date createTime;
    private Date lockedTime;
    private Long lockedBy;
}
