package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerRegistrationVo {
    private Long registrationId;
    private String registrationNo;
    private String tournamentName;
    private String seasonName;
    private String eventName;
    private String platformCode;
    private String teamName;
    private Integer registrationStatus;
    private String registrationStatusName;
    private String auditStatusName;
    private Date submittedTime;
}
