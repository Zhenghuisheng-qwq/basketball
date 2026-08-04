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
    private Integer snapshotVersion;
    private String snapshotStatus;
    private String snapshotStatusName;
    private Integer isCurrent;
    private Boolean mutable;
    private Boolean lockable;
    private Boolean locked;
    private String formatSummary;
    private List<BasketDivisionStageVo> stages;
}
