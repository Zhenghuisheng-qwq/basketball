package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketFormatTemplateTieBreakRule {
    private Long id;
    private Long templateId;
    private String stageCode;
    private String ruleCode;
    private Integer priorityNo;
    private Integer enabled;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
