package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketAnnouncementRuleLink {
    private Long id;
    private Long tournamentId;
    private Long seasonId;
    private Long oldAnnouncementId;
    private Long newAnnouncementId;
    private String ruleVersion;
    private Date effectiveTime;
    private Date invalidTime;
    private Integer currentFlag;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
