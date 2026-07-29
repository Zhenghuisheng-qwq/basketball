package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketCaptainGrantLog {
    private Long id;
    private Long grantId;
    private Long memberId;
    private Long playerId;
    private Long teamId;
    private Long seasonId;
    private String actionType;
    private Integer beforeStatus;
    private Integer afterStatus;
    private Integer paymentConfirmed;
    private String paymentReference;
    private Date grantTime;
    private Date expireTime;
    private Date revokeTime;
    private Long operatorId;
    private String operatorName;
    private String operatorType;
    private String reason;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
