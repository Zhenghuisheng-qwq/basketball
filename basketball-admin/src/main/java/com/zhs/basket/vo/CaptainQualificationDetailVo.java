package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class CaptainQualificationDetailVo {
    private Long id;
    private String qualificationNo;
    private Long memberId;
    private Long playerId;
    private Integer qualificationStatus;
    private Integer isCurrent;
    private Integer paymentConfirmed;
    private String paymentReference;
    private String grantReason;
    private Date grantedTime;
    private Date validFrom;
    private Date validTo;
    private String revokeReason;
}
