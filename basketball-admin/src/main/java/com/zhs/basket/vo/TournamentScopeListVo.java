package com.zhs.basket.vo;

import lombok.Data;

import java.util.Date;

@Data
public class TournamentScopeListVo {
    private Long id;
    private Long adminUserId;
    private String adminUserName;
    private Long tournamentId;
    private String tournamentName;
    private String scopeCode;
    private Long grantedBy;
    private String grantedByName;
    private Date grantedTime;
    private Date expireTime;
    private Integer activeFlag;
}
