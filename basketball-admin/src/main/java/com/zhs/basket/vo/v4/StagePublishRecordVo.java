package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class StagePublishRecordVo {
    private Long id;
    private Long stageId;
    private Long structureBatchId;
    private Integer structureExecutionVersion;
    private Integer scheduleRevision;
    private Integer publishNo;
    private String actionType;
    private String publishStatus;
    private String scheduleFingerprint;
    private Long operatorId;
    private Date operationTime;
    private String reason;
    private Integer isCurrent;
}
