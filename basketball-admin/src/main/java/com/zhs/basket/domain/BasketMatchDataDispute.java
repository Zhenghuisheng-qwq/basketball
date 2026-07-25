package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchDataDispute {
    private Long id;
    private String disputeNo;
    private Long matchId;
    private String initiatorType;
    private Long initiatorId;
    private String disputeType;
    private String originalValue;
    private String expectedValue;
    private String disputeReason;
    private String evidenceFileIds;
    private String disputeStatus;
    private Long handlerId;
    private Date handleTime;
    private String handleResult;
    private String handleRemark;
    private Long revisionId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
