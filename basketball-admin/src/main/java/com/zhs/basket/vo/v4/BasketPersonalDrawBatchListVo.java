package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPersonalDrawBatchListVo {
    private Long drawBatchId;
    private Long divisionId;
    private String divisionName;
    private String batchCode;
    private String batchName;
    private Integer batchVersion;
    private String batchStatus;
    private String resultSource;
    private String resultRemark;
    private String unassignedPolicy;
    private Date confirmedTime;
    private Date createTime;
}
