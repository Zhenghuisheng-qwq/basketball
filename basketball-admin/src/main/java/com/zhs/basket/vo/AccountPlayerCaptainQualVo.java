package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerCaptainQualVo {
    private Long qualificationId;
    private String qualificationNo;
    private Integer qualificationStatus;
    private String qualificationStatusName;
    private Integer isCurrent;
    private Date grantedTime;
    private Date validFrom;
    private Date validTo;
    private String grantReason;
    private Integer runtimeValid;
    private String runtimeInvalidReason;
}
