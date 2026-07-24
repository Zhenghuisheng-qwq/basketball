package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketHonorLineup {
    private Long id;
    private Long honorRecordId;
    private Long teamId;
    private Long playerId;
    private Long memberId;
    private String roleType;
    private String jerseyNo;
    private String positionCode;
    private Integer sortNo;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
