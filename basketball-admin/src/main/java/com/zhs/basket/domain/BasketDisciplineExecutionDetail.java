package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDisciplineExecutionDetail {
    private Long id;
    private Long disciplineRecordId;
    private String actionType;
    private String targetType;
    private Long targetId;
    private String executionStatus;
    private String resultMessage;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
