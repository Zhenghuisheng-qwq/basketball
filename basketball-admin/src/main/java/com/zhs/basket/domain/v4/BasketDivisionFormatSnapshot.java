package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * Division 规则快照。业务语义 = Rule Version，不另建平行 RuleVersion 表。
 */
@Data
public class BasketDivisionFormatSnapshot {
    private Long id;
    private Long divisionId;
    private Long sourceTemplateId;
    private String sourceTemplateCode;
    private String sourceTemplateName;
    private Integer sourceTemplateVersion;
    private String sourceTemplateType;
    private String registrationMode;
    private Integer snapshotVersion;
    private String snapshotStatus;
    private String rosterWorkflowMode;
    private Integer isCurrent;
    private String activeUniqueKey;
    private Long supersededSnapshotId;
    private Date supersededTime;
    private Date lockedTime;
    private Long lockedBy;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
