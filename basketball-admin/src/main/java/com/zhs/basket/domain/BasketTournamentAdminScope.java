package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketTournamentAdminScope {
    private Long id;
    private Long adminUserId;
    private Long tournamentId;
    private String scopeCode;
    private Long grantedBy;
    private Date grantedTime;
    private Date expireTime;
    private Integer activeFlag;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
