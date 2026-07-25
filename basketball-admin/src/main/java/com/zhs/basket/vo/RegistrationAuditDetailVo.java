package com.zhs.basket.vo;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class RegistrationAuditDetailVo {
    private Long id;
    private String registrationNo;
    private Long seasonId;
    private Long teamId;
    private String tournamentName;
    private String teamName;
    private String captainName;
    private Long captainMemberId;
    private String contactName;
    private String contactMobile;
    private Integer registrationStatus;
    private String registrationStatusName;
    private Integer lineupCount;
    private String remark;
    private String rejectReason;
    private Date submittedTime;
    private Date auditTime;
    private List<RegistrationPlayerVo> players;
}
