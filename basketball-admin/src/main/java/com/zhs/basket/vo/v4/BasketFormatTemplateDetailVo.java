package com.zhs.basket.vo.v4;

import lombok.Data;
import java.util.List;

@Data
public class BasketFormatTemplateDetailVo {
    private Long id;
    private Long seriesId;
    private String templateCode;
    private String templateName;
    private Integer versionNo;
    private String templateDescription;
    private Integer enabled;
    private Integer isDefault;
    private List<BasketFormatTemplateStageVo> stages;
    private List<BasketFormatTemplateTieBreakRuleVo> tieBreakRules;
}
