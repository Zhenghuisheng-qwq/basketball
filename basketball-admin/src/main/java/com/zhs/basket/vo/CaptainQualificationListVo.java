package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class CaptainQualificationListVo {
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
    private Date grantedTime;
    private Date validFrom;
    private Date validTo;
    /** 运行时是否有效（与前台 canCreateTeam 规则一致） */
    private Integer runtimeValid;
    /** 运行时无效原因 */
    private String runtimeInvalidReason;
}
