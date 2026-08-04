package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivisionGroupTeam;
import com.zhs.basket.vo.v4.BasketDivisionGroupTeamVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDivisionGroupTeamMapper {
    BasketDivisionGroupTeam selectById(@Param("id") Long id);

    List<BasketDivisionGroupTeam> selectByBatchId(@Param("groupingBatchId") Long groupingBatchId);

    List<BasketDivisionGroupTeam> selectByGroupId(@Param("groupId") Long groupId);

    List<BasketDivisionGroupTeamVo> selectVoByGroupId(@Param("groupId") Long groupId);

    int countByGroupId(@Param("groupId") Long groupId);

    int insert(BasketDivisionGroupTeam entity);

    int updateById(BasketDivisionGroupTeam entity);

    int softDelete(@Param("id") Long id);
}
