package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatchTeamStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketMatchTeamStat
 */
public interface BasketV4MatchTeamStatMapper {
    BasketMatchTeamStat selectById(@Param("id") Long id);
    List<BasketMatchTeamStat> selectList(BasketMatchTeamStat query);
    int insert(BasketMatchTeamStat entity);
    int updateById(BasketMatchTeamStat entity);
    int softDelete(@Param("id") Long id);
}
