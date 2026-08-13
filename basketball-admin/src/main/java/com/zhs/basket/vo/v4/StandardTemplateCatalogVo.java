package com.zhs.basket.vo.v4;

import com.zhs.basket.support.v4.template.StandardTemplateDefinition;
import lombok.Data;

import java.util.List;

@Data
public class StandardTemplateCatalogVo {
    private String seriesCode;
    private String defaultTemplateCode;
    private List<StandardTemplateDefinition> templates;
}
