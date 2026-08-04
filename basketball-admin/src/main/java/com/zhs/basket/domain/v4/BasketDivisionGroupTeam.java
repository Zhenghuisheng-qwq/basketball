package com.zhs.basket.domain.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionGroupTeam {
    private Long id;
    private Long groupingBatchId;
    private Long divisionId;
    private Long stageId;
    private Long groupId;
    private Long eventTeamId;
    private Integer seedNo;
    private Integer drawOrder;
    private String assignmentSource;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
