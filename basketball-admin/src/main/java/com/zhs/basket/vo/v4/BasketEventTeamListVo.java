package com.zhs.basket.vo.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketEventTeamListVo {
    private Long id;
    private Long seasonId;
    private String seasonTitle;
    private Long divisionId;
    private String divisionName;
    private String platformCode;
    private Long registrationId;
    private Long sourceTeamId;
    private String teamNameSnapshot;
    private Long teamLogoFileId;
    private String teamShortName;
    private Long captainPlayerId;
    private String captainPlayerName;
    private String eventTeamNo;
    private String eventTeamStatus;
    private Date createTime;
}
