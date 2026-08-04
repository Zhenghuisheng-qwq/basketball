package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionGroupingBatch {
    private Long id;
    private Long divisionId;
    private Long stageId;
    private Integer batchVersion;
    private String groupingMode;
    private String groupingStatus;
    private String activeUniqueKey;
    private Integer groupCount;
    private String resultSource;
    private Long confirmedBy;
    private Date confirmedTime;
    private Long createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
