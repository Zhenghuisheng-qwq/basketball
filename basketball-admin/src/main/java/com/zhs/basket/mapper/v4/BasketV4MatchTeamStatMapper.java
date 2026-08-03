package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketV4MatchTeamStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketV4MatchTeamStat
 */
public interface BasketV4MatchTeamStatMapper {
    BasketV4MatchTeamStat selectById(@Param("id") Long id);
    List<BasketV4MatchTeamStat> selectList(BasketV4MatchTeamStat query);
    int insert(BasketV4MatchTeamStat entity);
    int updateById(BasketV4MatchTeamStat entity);
    int softDelete(@Param("id") Long id);
}
