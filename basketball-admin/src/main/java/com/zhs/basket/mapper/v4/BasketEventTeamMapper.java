package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.query.v4.BasketEventTeamQuery;
import com.zhs.basket.vo.v4.BasketEventTeamListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketEventTeamMapper {
    BasketEventTeam selectById(@Param("id") Long id);

    List<BasketEventTeam> selectList(BasketEventTeam query);

    List<BasketEventTeamListVo> selectVoList(BasketEventTeamQuery query);

    BasketEventTeam selectByDivisionSourceTeam(@Param("divisionId") Long divisionId, @Param("sourceTeamId") Long sourceTeamId);

    List<BasketEventTeam> selectActiveByDivisionId(@Param("divisionId") Long divisionId);

    int insert(BasketEventTeam entity);

    int updateById(BasketEventTeam entity);

    int softDelete(@Param("id") Long id);
}
