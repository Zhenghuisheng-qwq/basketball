package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentRule {
    private Long id;
    private Long seasonId;
    private String ruleType;
    private String ruleTitle;
    private String ruleContent;
    private String versionNo;
    private Date effectiveTime;
    private Integer ruleStatus;
    private Integer displayOrder;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
