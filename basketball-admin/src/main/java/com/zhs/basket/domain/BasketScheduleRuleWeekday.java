package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketScheduleRuleWeekday {
    private Long id;
    private Long ruleId;
    private Integer weekday;
    private Integer sort;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
