package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class CaptainQualificationListVo {
    private Long id;
    private String qualificationNo;
    private Long memberId;
    private Long playerId;
    private Integer qualificationStatus;
    private Integer isCurrent;
    private Integer paymentConfirmed;
    private Date grantedTime;
    private Date validFrom;
    private Date validTo;
    /** 运行时是否有效（与前台 canCreateTeam 规则一致） */
    private Integer runtimeValid;
    /** 运行时无效原因 */
    private String runtimeInvalidReason;
}
