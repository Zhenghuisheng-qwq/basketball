package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class MatchDisputeListVo {
    private Long id;
    private String disputeNo;
    private Long matchId;
    private String matchNo;
    private String disputeType;
    private String disputeStatus;
    private String initiatorType;
    private Long initiatorId;
    private String disputeReason;
    private Date createTime;
    private Date handleTime;
    private String handleResult;
}
