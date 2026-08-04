package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketFormatTemplateTieBreakRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketFormatTemplateTieBreakRuleMapper {
    List<BasketFormatTemplateTieBreakRule> selectByTemplateId(@Param("templateId") Long templateId);
}
