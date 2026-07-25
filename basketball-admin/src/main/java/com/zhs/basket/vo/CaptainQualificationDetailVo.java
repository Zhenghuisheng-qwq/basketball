package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class CaptainQualificationDetailVo {
    private Long id;
    private String qualificationNo;
    /** 用户ID（历史字段 memberId） */
    private Long memberId;
    private Long userId;
    private Long playerId;
    private String username;
    private String email;
    private String nickname;
    private String playerNo;
    private String gameAccount;
    private String positionCode;
    private String certificationStatusName;
    private Integer qualificationStatus;
    private Integer isCurrent;
    private Integer paymentConfirmed;
    private String paymentReference;
    private String grantReason;
    private Date grantedTime;
    private Date validFrom;
    private Date validTo;
    private String revokeReason;
    private Integer runtimeValid;
    private String runtimeInvalidReason;
}
