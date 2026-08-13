package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketPersonalDrawAssignment;
import com.zhs.basket.vo.v4.BasketPersonalDrawAssignmentVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPersonalDrawAssignmentMapper {
    BasketPersonalDrawAssignment selectById(@Param("id") Long id);

    List<BasketPersonalDrawAssignment> selectByTeamId(@Param("drawTeamId") Long drawTeamId);

    List<BasketPersonalDrawAssignment> selectByBatchId(@Param("drawBatchId") Long drawBatchId);

    List<BasketPersonalDrawAssignmentVo> selectVoByTeamId(@Param("drawTeamId") Long drawTeamId);

    int insert(BasketPersonalDrawAssignment entity);

    int updateById(BasketPersonalDrawAssignment entity);

    int softDelete(@Param("id") Long id);

    int softDeleteByTeamId(@Param("drawTeamId") Long drawTeamId);

    int softDeleteByBatchId(@Param("drawBatchId") Long drawBatchId);

    BasketPersonalDrawAssignment selectByPersonalRegistrationId(@Param("personalRegistrationId") Long personalRegistrationId);
}
