package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BasketPersonalDrawBatchDetailVo {
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
    private String unassignedRemark;
    private Date confirmedTime;
    private List<BasketPersonalDrawTeamVo> teams;
}
