package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class AccountPlayerPlatformAccountVo {
    private Long id;
    private String platform;
    private String displayGameId;
    private String normalizedGameId;
    private Integer accountStatus;
    private String accountStatusName;
    private Integer isPrimary;
    private Date approvedTime;
    private Date effectiveTime;
    private Date invalidTime;
    private Date updateTime;
}
