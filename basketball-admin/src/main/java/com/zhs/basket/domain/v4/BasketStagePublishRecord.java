package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketStagePublishRecord {
    private Long id;
    private Long divisionId;
    private Long stageId;
    private Long structureBatchId;
    private Integer structureExecutionVersion;
    private Long ruleVersionId;
    private Long rosterVersionId;
    private Long pairingExecutionId;
    private Integer scheduleRevision;
    private Integer publishNo;
    private String actionType;
    private String publishStatus;
    private String scheduleFingerprint;
    private Long operatorId;
    private Date operationTime;
    private String reason;
    private Integer isCurrent;
    private String currentUniqueKey;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
