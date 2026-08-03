package com.zhs.basket.vo.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketEventPlayerListVo {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long eventTeamId;
    private String teamNameSnapshot;
    private Long sourcePlayerId;
    private String playerNameSnapshot;
    private String gameIdSnapshot;
    private String platformCode;
    private String position;
    private String jerseyNo;
    private Integer isCaptain;
    private Integer isStarter;
    private String eventPlayerStatus;
    private Date createTime;
}
