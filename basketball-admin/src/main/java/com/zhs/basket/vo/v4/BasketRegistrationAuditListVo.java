package com.zhs.basket.vo.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketRegistrationAuditListVo {
    private Long registrationId;
    private String seasonTitle;
    private String divisionName;
    private String platformCode;
    private Long teamId;
    private String teamName;
    private Long captainPlayerId;
    private String captainPlayerName;
    private Integer lineupCount;
    private String registrationStatus;
    private Date submitTime;
}
