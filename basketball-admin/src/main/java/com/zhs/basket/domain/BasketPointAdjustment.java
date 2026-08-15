package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPointAdjustment {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long stageId;
    private Long groupId;
    private Long teamId;
    private Long eventTeamId;
    private Long disciplineId;
    private String adjustmentType;
    private Integer adjustmentPoints;
    private String adjustmentReason;
    private String remark;
    private String idempotencyKey;
    private Long operatorId;
    private String operatorName;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
