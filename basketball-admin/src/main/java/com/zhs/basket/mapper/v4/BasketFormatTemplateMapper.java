package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketFormatTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketFormatTemplateMapper {
    BasketFormatTemplate selectById(@Param("id") Long id);

    BasketFormatTemplate selectByCodeVersion(@Param("templateCode") String templateCode,
                                             @Param("versionNo") Integer versionNo);

    List<BasketFormatTemplate> selectEnabledBySeriesId(@Param("seriesId") Long seriesId);

    BasketFormatTemplate selectDefaultBySeriesId(@Param("seriesId") Long seriesId);

    int insert(BasketFormatTemplate entity);
}
