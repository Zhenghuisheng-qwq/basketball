package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionGroup {
    private Long id;
    private Long groupingBatchId;
    private Long divisionId;
    private Long stageId;
    private String groupCode;
    private String groupName;
    private Integer groupOrder;
    private Integer qualifyCount;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
