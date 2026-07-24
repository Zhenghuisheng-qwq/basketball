package com.zhs.basket.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TeamRankListVo {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private String rankType;
    private String rankTypeName;
    private Long teamId;
    private String teamNo;
    private String teamName;
    private String shortName;
    private String logoUrl;
    private Integer rankNo;
    private Integer previousRank;
    private Integer rankChange;
    private BigDecimal statValue;
    private Integer games;
    private Date snapshotDate;
}
