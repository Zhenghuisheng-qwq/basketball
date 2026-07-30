package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class MatchExceptionListVo {
    private Long id;
    private String applicationNo;
    private Long matchId;
    private String matchNo;
    private String exceptionType;
    private String applicationStatus;
    private String decisionType;
    private Long applicantTeamId;
    private String exceptionReason;
    private Date createTime;
    private Date processedTime;
}
