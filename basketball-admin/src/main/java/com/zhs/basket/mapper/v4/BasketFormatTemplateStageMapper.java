package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketFormatTemplateStage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketFormatTemplateStageMapper {
    List<BasketFormatTemplateStage> selectByTemplateId(@Param("templateId") Long templateId);
}
