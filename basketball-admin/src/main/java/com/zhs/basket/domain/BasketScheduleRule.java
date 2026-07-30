package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketScheduleRule {
    private Long id;
    private String ruleName;
    private String ruleCode;
    private String ruleVersion;
    private String eventType;
    private String platformCode;
    private Integer enabled;
    /** 每个时间段最大比赛场次 */
    private Integer slotCapacity;
    /** 时间槽分配策略：FILL_FIRST / EVEN_SPLIT */
    private String allocationMode;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
