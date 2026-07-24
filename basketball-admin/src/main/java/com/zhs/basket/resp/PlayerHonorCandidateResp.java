package com.zhs.basket.resp;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlayerHonorCandidateResp {
    private Long playerId;
    private String playerNo;
    private String displayName;
    private String avatarUrl;
    private Long teamId;
    private String teamName;
    private Integer rankNo;
    private BigDecimal statValue;
    private Integer games;
    private String rankType;
    private String awardType;
}
