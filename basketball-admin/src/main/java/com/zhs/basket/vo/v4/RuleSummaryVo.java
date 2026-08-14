package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class RuleSummaryVo {
    private Long ruleVersionId;
    private Integer ruleVersionNo;
    private String ruleStatus;
    private String ruleStatusName;
    private boolean locked;
    private String templateType;
    private String templateCode;
}
