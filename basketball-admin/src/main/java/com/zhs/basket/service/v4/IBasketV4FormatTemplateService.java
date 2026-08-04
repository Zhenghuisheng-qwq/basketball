package com.zhs.basket.service.v4;

import com.zhs.basket.vo.v4.BasketFormatTemplateDetailVo;
import com.zhs.basket.vo.v4.BasketFormatTemplateListVo;

import java.util.List;

public interface IBasketV4FormatTemplateService {
    List<BasketFormatTemplateListVo> listEnabledTemplates(Long seriesId);

    BasketFormatTemplateDetailVo getTemplateDetail(Long templateId);
}
