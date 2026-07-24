package com.zhs.basket.req;

import java.util.Date;
import lombok.Data;

@Data
public class TournamentRuleCreateReq {
    private String ruleType;
    private String ruleTitle;
    private String ruleContent;
    private String versionNo;
    private Date effectiveTime;
    private Integer ruleStatus;
    private Integer displayOrder;
}
