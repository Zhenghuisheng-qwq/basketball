package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketCaptainQualification {
    private Long id;
    private String qualificationNo;
    private Long memberId;
    private Long playerId;
    private Integer qualificationStatus;
    private Integer isCurrent;
    private Integer paymentConfirmed;
    private String paymentReference;
    private String grantReason;
    private Long grantedBy;
    private Date grantedTime;
    private Date validFrom;
    private Date validTo;
    private Long revokedBy;
    private Date revokedTime;
    private String revokeReason;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
