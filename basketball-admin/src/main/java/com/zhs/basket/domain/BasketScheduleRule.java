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
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
