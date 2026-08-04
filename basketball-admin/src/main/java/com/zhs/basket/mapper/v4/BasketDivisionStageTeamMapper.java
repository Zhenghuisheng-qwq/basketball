package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDivisionStageTeamMapper {
    List<BasketDivisionStageTeam> selectByStageId(@Param("stageId") Long stageId);

    BasketDivisionStageTeam selectById(@Param("id") Long id);

    int insert(BasketDivisionStageTeam entity);

    int batchInsert(@Param("list") List<BasketDivisionStageTeam> list);

    int updateById(BasketDivisionStageTeam entity);

    int softDeleteByStageId(@Param("stageId") Long stageId);

    int countByStageId(@Param("stageId") Long stageId);

    boolean existsByStageAndEventTeam(@Param("stageId") Long stageId, @Param("eventTeamId") Long eventTeamId);
}
