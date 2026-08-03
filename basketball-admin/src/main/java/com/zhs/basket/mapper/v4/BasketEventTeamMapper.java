package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketEventTeam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketEventTeam
 */
public interface BasketEventTeamMapper {
    BasketEventTeam selectById(@Param("id") Long id);
    List<BasketEventTeam> selectList(BasketEventTeam query);
    int insert(BasketEventTeam entity);
    int updateById(BasketEventTeam entity);
    int softDelete(@Param("id") Long id);
}
