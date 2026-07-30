package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketScheduleRuleTimeSlot {
    private Long id;
    private Long ruleId;
    private String startTime;
    private Integer sort;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
