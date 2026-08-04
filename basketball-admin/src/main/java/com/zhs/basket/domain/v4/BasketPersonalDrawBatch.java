package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPersonalDrawBatch {
    private Long id;
    private Long divisionId;
    private String batchCode;
    private String batchName;
    private Integer batchVersion;
    private String batchStatus;
    private String activeUniqueKey;
    private String resultSource;
    private String resultRemark;
    private String unassignedPolicy;
    private String unassignedRemark;
    private Long confirmedBy;
    private Date confirmedTime;
    private Long createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
