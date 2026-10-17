package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 已发布规则重新发布前的影响预览。不写库。
 */
@Data
public class FormatRevisionPreviewVo {
    private boolean allowed;
    private List<String> blockers = new ArrayList<>();
    private String currentTemplateCode;
    private String currentTemplateType;
    private String currentRuleVersionLabel;
    private String nextTemplateCode;
    private String nextFormatSummary;
    private int unstartedMatchCount;
    private int matchSeriesCount;
    private int scheduleBatchCount;
    private boolean scheduleWillBeInvalidated;
    private String scheduleImpact;
    private String preservedScope;
}
