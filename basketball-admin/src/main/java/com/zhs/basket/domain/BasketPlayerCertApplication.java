package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketPlayerCertApplication {
    private Long id;
    private String applicationNo;
    private Long memberId;
    private Long playerId;
    private String applicationType;
    private String gameTerminal;
    private String gameAccount;
    private String gameNickname;
    private String positionCode;
    private Integer applicationStatus;
    private Date submittedTime;
    private Date reviewedTime;
    private Long currentAuditorId;
    private String reviewRemark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
