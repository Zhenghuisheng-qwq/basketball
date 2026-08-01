package com.zhs.basket.query;

import lombok.Data;

@Data
public class ScheduleMatchQuery {
    private Long seasonId;
    private Long stageId;
    private String platformCode;
    private Integer matchStatus;
    private Integer publishStatus;
    private Long scheduleBatchId;
    /** 为 true 时排除无批次/无主客队/已结束已取消的历史脏数据 */
    private Boolean excludeLegacy;
    private String beginTime;
    private String endTime;
}
