package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketTeamStat;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketTeamStat
 */
public interface BasketTeamStatMapper {
    BasketTeamStat selectById(@Param("id") Long id);
    List<BasketTeamStat> selectList(BasketTeamStat query);
    List<BasketTeamStat> selectByDivisionId(@Param("divisionId") Long divisionId);
    int deleteByDivisionId(@Param("divisionId") Long divisionId);
    int insert(BasketTeamStat entity);
    int updateById(BasketTeamStat entity);
    int softDelete(@Param("id") Long id);
}
