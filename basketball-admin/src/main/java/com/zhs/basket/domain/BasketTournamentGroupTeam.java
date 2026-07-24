package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentGroupTeam {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private Long groupId;
    private Long teamId;
    private Integer seedNo;
    private Integer drawOrder;
    private Integer groupTeamStatus;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
