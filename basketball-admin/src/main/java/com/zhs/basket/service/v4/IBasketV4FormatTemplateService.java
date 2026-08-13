package com.zhs.basket.service.v4;

import com.zhs.basket.vo.v4.BasketFormatTemplateDetailVo;
import com.zhs.basket.vo.v4.BasketFormatTemplateListVo;
import com.zhs.basket.vo.v4.StandardTemplateCatalogVo;
import com.zhs.basket.support.v4.template.StandardTemplateDefinition;

import java.util.List;

public interface IBasketV4FormatTemplateService {
    List<BasketFormatTemplateListVo> listEnabledTemplates(Long seriesId);

    BasketFormatTemplateDetailVo getTemplateDetail(Long templateId);

    StandardTemplateCatalogVo getStandardCatalog(Long seriesId);

    StandardTemplateDefinition getStandardCapabilities(String templateCode);
}
