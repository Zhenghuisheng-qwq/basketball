package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class PlayerPlatformChangeDetailVo {
    private Long id;
    private String requestNo;
    private Long playerId;
    private Long memberId;
    private String nickname;
    private String email;
    private String playerNo;
    private String changeType;
    private String changeTypeName;
    private String platform;
    private Long platformAccountId;
    private String oldDisplayGameId;
    private String newDisplayGameId;
    private String newNormalizedGameId;
    private Long proofFileId;
    private Integer requestStatus;
    private String requestStatusName;
    private Date submitTime;
    private Date reviewedTime;
    private Long reviewerId;
    private String reviewRemark;
    private Long resultPlatformAccountId;
}
