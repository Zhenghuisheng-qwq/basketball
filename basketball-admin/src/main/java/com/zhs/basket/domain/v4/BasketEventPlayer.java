package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketEventPlayer
 */
@Data
public class BasketEventPlayer {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long rosterVersionId;
    private Long eventTeamId;
    private Long sourcePlayerId;
    private Long memberId;
    private Long platformIdentityId;
    private String platformCode;
    private String gameIdSnapshot;
    private String playerNameSnapshot;
    private Long avatarFileId;
    private String position;
    private String jerseyNo;
    private Integer isCaptain;
    private Integer isStarter;
    private String eventPlayerStatus;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
