package com.zhs.basket.resp;

import lombok.Data;

import java.util.Date;

@Data
public class HonorGenerationBatchResp {
    private Long id;
    private String batchNo;
    private Long seasonId;
    private String seasonName;
    private Long settlementBatchId;
    private String batchStatus;
    private Integer honorCount;
    private Long operatorId;
    private String errorMessage;
    private Integer currentFlag;
    private Date createTime;
    private Date updateTime;
}
