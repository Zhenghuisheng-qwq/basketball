package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketHonorGenerationBatch {
    private Long id;
    private String batchNo;
    private Long seasonId;
    private Long settlementBatchId;
    private String batchStatus;
    private Integer honorCount;
    private Long operatorId;
    private String errorMessage;
    private Integer currentFlag;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
