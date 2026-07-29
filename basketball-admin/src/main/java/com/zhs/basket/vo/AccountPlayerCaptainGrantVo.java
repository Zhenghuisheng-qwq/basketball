package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerCaptainGrantVo {
    private Long qualificationId;
    private String qualificationNo;
    private Integer qualificationStatus;
    private String qualificationStatusName;
    private String seasonName;
    private String tournamentName;
    private String eventName;
    private Long teamId;
    private String teamName;
    private Integer paymentConfirmed;
    private String paymentReference;
    private Date grantedTime;
    private Date validTo;
    private Long grantedBy;
    private String grantedByName;
    private Date revokedTime;
    private String revokeReason;
}
