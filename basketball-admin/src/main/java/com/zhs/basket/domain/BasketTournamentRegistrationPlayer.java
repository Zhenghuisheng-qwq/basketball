package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentRegistrationPlayer {
    private Long id;
    private Long registrationId;
    private Long seasonId;
    private Long teamId;
    private Long playerId;
    private Long memberId;
    private Long teamMemberId;
    private String rosterRole;
    private String jerseyNo;
    private Integer isStarting;
    private Integer isCaptain;
    private Integer validationStatus;
    private String rejectReason;
    private Integer sortNo;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
