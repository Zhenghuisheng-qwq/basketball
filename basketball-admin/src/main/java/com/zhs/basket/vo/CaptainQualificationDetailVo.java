package com.zhs.basket.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import lombok.Data;

@Data
public class CaptainQualificationDetailVo {
    private Long id;
    private String qualificationNo;
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
    private Long seasonId;
    private String seasonName;
    private String tournamentName;
    private Long teamId;
    private String teamName;
    private Integer qualificationStatus;
    private Integer isCurrent;
    private Integer paymentConfirmed;
    private String paymentReference;
    private String grantReason;
    private Date grantedTime;
    private Date validFrom;
    private Date validTo;
    private Long grantedBy;
    private String grantedByName;
    private Date revokedTime;
    private Long revokedBy;
    private String revokedByName;
    private String revokeReason;
    private Integer runtimeValid;
    private String runtimeInvalidReason;
}
