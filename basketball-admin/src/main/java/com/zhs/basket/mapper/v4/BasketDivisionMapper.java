package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.query.v4.BasketDivisionQuery;
import com.zhs.basket.vo.v4.BasketDivisionListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketDivision
 */
public interface BasketDivisionMapper {
    BasketDivision selectById(@Param("id") Long id);

    List<BasketDivisionListVo> selectVoList(BasketDivisionQuery query);

    List<BasketDivision> selectBySeasonId(@Param("seasonId") Long seasonId);

    BasketDivision selectBySeasonPlatform(@Param("seasonId") Long seasonId,
                                          @Param("platformCode") String platformCode,
                                          @Param("excludeId") Long excludeId);

    int countBySeasonId(@Param("seasonId") Long seasonId);

    int insert(BasketDivision entity);

    int updateById(BasketDivision entity);

    int softDelete(@Param("id") Long id);

    List<BasketDivision> selectWithoutFormatSnapshot();
}
