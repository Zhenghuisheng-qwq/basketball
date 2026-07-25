package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class RegistrationAuditListVo {
    private Long id;
    private String registrationNo;
    private Long seasonId;
    private Long teamId;
    private String tournamentName;
    private String teamName;
    private String captainName;
    private Integer registrationStatus;
    private String registrationStatusName;
    private Integer lineupCount;
    private Date submittedTime;
}
