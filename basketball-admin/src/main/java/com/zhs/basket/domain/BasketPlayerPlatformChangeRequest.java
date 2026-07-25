package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketPlayerPlatformChangeRequest {
    private Long id;
    private String requestNo;
    private Long playerId;
    private Long memberId;
    private String changeType;
    private String platform;
    private Long platformAccountId;
    private String oldDisplayGameId;
    private String newDisplayGameId;
    private String newNormalizedGameId;
    private Long proofFileId;
    private Integer requestStatus;
    private Date submitTime;
    private Date reviewedTime;
    private Long reviewerId;
    private String reviewRemark;
    private Long resultPlatformAccountId;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
