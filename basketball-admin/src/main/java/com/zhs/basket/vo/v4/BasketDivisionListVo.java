package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionListVo {
    private Long id;
    private Long seasonId;
    private String seasonTitle;
    private String seriesCode;
    private String seriesName;
    private String platformCode;
    private String divisionName;
    private String registrationMode;
    private String formatType;
    private Integer maxTeamCount;
    private Integer maxPersonCount;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    private String divisionStatus;
    private Integer sortOrder;
    private Date createTime;
    private Date updateTime;
}
