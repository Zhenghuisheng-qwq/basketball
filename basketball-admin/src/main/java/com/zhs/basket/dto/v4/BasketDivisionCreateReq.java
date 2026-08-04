package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketDivisionCreateReq {
    private Long seasonId;
    private String platformCode;
    private String gameMode;
    private String divisionName;
    private String registrationMode;
    private String formatType;
    private Long formatTemplateId;
    private Integer maxTeamCount;
    private Integer maxPersonCount;
    private Integer lineupMinCount;
    private Integer lineupMaxCount;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    private Integer sortOrder;
}
