package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionRosterVersion {
    private Long id;
    private Long divisionId;
    private Integer versionNo;
    private String registrationMode;
    private Long ruleVersionId;
    private String status;
    private Integer isCurrent;
    private String activeUniqueKey;
    private Integer formalCount;
    private Integer candidateCount;
    private Integer excludedCount;
    private String allocationFingerprint;
    private Date lockedTime;
    private Long lockedBy;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
