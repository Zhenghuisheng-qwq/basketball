package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class TournamentScopeGrantReq {
    private Long adminUserId;
    private Long tournamentId;
    private String scopeCode;
    private Date expireTime;
}
