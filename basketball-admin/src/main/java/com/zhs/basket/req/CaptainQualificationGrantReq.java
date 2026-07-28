package com.zhs.basket.req;

import lombok.Data;

@Data
public class CaptainQualificationGrantReq {
    /** 授权赛事赛季主键（必填） */
    private Long seasonId;
    /** 球员档案主键（必填） */
    private Long playerId;
    /** 关联球队（可选） */
    private Long teamId;
    /** 缴费确认：0 否，1 是（必填且必须为 1） */
    private Integer paymentConfirmed;
    private String paymentReference;
    private String grantRemark;
}
