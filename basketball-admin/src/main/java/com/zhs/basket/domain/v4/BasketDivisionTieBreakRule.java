package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketDivisionTieBreakRule {
    private Long id;
    private Long snapshotId;
    private Long divisionId;
    private Long stageId;
    private String ruleCode;
    private Integer priorityNo;
    private Integer enabled;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
