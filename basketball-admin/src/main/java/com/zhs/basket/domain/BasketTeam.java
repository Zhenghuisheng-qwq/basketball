package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTeam {
    private Long id;
    private String teamNo;
    private String teamName;
    private String shortName;
    private String englishName;
    private Long logoFileId;
    private String regionCode;
    private String regionName;
    private String gameTerminal;
    private String description;
    private Long contactMemberId;
    private Long captainMemberId;
    private Long captainPlayerId;
    /** 所属赛事 Division ID；NULL 表示 Legacy 历史球队 */
    private Long divisionId;
    private Integer maxMemberCount;
    private Integer currentMemberCount;
    private Integer teamStatus;
    private Integer auditStatus;
    private Integer publicStatus;
    private Date establishedDate;
    private Date dissolvedTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
