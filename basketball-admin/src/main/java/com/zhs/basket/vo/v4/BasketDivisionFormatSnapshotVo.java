package com.zhs.basket.vo.v4;

import lombok.Data;
import java.util.List;

@Data
public class BasketDivisionFormatSnapshotVo {
    private Long snapshotId;
    private Long divisionId;
    private Long sourceTemplateId;
    private String sourceTemplateCode;
    private String sourceTemplateName;
    private Integer sourceTemplateVersion;
    private String sourceTemplateType;
    private String sourceTemplateTypeName;
    private String registrationMode;
    private String registrationModeName;
    private Integer snapshotVersion;
    private String snapshotStatus;
    /** 兼容字段：Rule Version = snapshotId */
    private Long ruleVersionId;
    /** 兼容字段：Rule Version 号 = snapshotVersion */
    private Integer ruleVersionNo;
    /** 兼容字段：Rule Version 状态 = snapshotStatus */
    private String ruleVersionStatus;
    private String snapshotStatusName;
    private Integer isCurrent;
    private Boolean mutable;
    private Boolean canApplyStandard;
    private Boolean lockable;
    private Boolean locked;
    private String formatSummary;
    private List<BasketDivisionStageVo> stages;
}
