package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketDivisionFormatSnapshot {
    private Long id;
    private Long divisionId;
    private Long sourceTemplateId;
    private String sourceTemplateCode;
    private String sourceTemplateName;
    private Integer sourceTemplateVersion;
    private Integer snapshotVersion;
    private String snapshotStatus;
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
