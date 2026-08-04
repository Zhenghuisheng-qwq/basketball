package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BasketDivisionGroupingBatchDetailVo {
    private Long groupingBatchId;
    private Long divisionId;
    private String divisionName;
    private Long stageId;
    private String stageCode;
    private String stageName;
    private Integer batchVersion;
    private String groupingMode;
    private String groupingStatus;
    private Integer groupCount;
    private String resultSource;
    private Date confirmedTime;
    private List<BasketDivisionGroupVo> groups;
}
