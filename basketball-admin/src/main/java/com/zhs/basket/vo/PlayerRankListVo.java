package com.zhs.basket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PlayerRankListVo {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private String rankType;
    private String rankTypeName;
    private Long playerId;
    private String playerNo;
    private String displayName;
    private String avatarUrl;
    private Long teamId;
    private String teamName;
    private Integer rankNo;
    private Integer previousRank;
    private Integer rankChange;
    private BigDecimal statValue;
    private Integer games;
    private Date snapshotDate;
}
