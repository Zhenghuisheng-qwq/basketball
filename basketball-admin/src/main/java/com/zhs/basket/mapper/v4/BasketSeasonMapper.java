package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.query.v4.BasketSeasonQuery;
import com.zhs.basket.vo.v4.BasketSeasonListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketSeason
 */
public interface BasketSeasonMapper {
    BasketSeason selectById(@Param("id") Long id);

    List<BasketSeasonListVo> selectVoList(BasketSeasonQuery query);

    BasketSeason selectBySeriesYearName(@Param("seriesId") Long seriesId,
                                        @Param("seasonYear") Integer seasonYear,
                                        @Param("seasonName") String seasonName,
                                        @Param("excludeId") Long excludeId);

    int countDivisionBySeasonId(@Param("seasonId") Long seasonId);

    int insert(BasketSeason entity);

    int updateById(BasketSeason entity);

    int softDelete(@Param("id") Long id);
}
