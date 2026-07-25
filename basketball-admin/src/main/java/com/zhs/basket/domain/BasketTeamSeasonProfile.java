package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTeamSeasonProfile {
    private Long id;
    private Long teamId;
    private Long seasonId;
    private Long registrationId;
    private String eventTeamNo;
    private String teamNameSnapshot;
    private String shortNameSnapshot;
    private String descriptionSnapshot;
    private String gameTerminal;
    private String registrationSource;
    private Integer participationStatus;
    private Integer rosterLockStatus;
    private Date rosterLockedTime;
    private Date approvedTime;
    private String displayName;
    private Long logoFileId;
    private Long captainPlayerId;
    private Integer registrationStatus;
    private Integer seedNo;
    private Long groupId;
    private Integer finalRank;
    private Integer seasonStatus;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
