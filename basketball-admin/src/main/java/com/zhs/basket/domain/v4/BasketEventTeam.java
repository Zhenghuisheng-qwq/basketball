package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketEventTeam
 */
@Data
public class BasketEventTeam {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long registrationId;
    private Long sourceTeamId;
    private String teamNameSnapshot;
    private Long teamLogoFileId;
    private String teamShortName;
    private Long captainPlayerId;
    private Long captainMemberId;
    private String eventTeamNo;
    private String eventTeamStatus;
    private String platformCode;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
