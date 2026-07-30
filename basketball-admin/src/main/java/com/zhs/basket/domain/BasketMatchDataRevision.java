package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchDataRevision {
    private Long id;
    private String revisionNo;
    private Long matchId;
    private Integer fromVersion;
    private Integer toVersion;
    private Long fromOfficialResultId;
    private Long toOfficialResultId;
    private String revisionReason;
    private String revisionSource;
    private Long disputeId;
    private Long operatorId;
    private Long auditorId;
    private Date effectiveTime;
    private String revisionStatus;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
