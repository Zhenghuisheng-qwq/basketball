package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPointAdjustment {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private Long groupId;
    private Long teamId;
    private Long disciplineId;
    private Integer adjustmentPoints;
    private String adjustmentReason;
    private String idempotencyKey;
    private Long operatorId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
