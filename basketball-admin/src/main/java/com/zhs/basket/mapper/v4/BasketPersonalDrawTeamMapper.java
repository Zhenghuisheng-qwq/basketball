package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketPersonalDrawTeam;
import com.zhs.basket.vo.v4.BasketPersonalDrawTeamVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPersonalDrawTeamMapper {
    BasketPersonalDrawTeam selectById(@Param("id") Long id);

    List<BasketPersonalDrawTeam> selectByBatchId(@Param("drawBatchId") Long drawBatchId);

    List<BasketPersonalDrawTeamVo> selectVoByBatchId(@Param("drawBatchId") Long drawBatchId);

    List<BasketPersonalDrawTeamVo> selectFormationTasksByCaptainUserId(@Param("captainUserId") Long captainUserId);

    BasketPersonalDrawTeamVo selectFormationDetail(@Param("drawTeamId") Long drawTeamId);

    int insert(BasketPersonalDrawTeam entity);

    int updateById(BasketPersonalDrawTeam entity);

    int softDelete(@Param("id") Long id);

    int refreshActualMemberCount(@Param("drawTeamId") Long drawTeamId);
}
