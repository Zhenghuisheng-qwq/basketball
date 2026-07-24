package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class TournamentRuleVo {
    private Long id;
    private Long seasonId;
    private String ruleType;
    private String ruleTitle;
    private String ruleContent;
    private String versionNo;
    private Date effectiveTime;
    private Integer ruleStatus;
    private Integer displayOrder;
}
