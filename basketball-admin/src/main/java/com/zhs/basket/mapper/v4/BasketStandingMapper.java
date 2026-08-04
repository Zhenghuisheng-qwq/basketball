package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketStanding;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketStanding
 */
public interface BasketStandingMapper {
    BasketStanding selectById(@Param("id") Long id);
    List<BasketStanding> selectList(BasketStanding query);
    List<BasketStanding> selectByDivisionId(@Param("divisionId") Long divisionId);
    int deleteByDivisionId(@Param("divisionId") Long divisionId);

    int deleteByStageScope(@Param("divisionId") Long divisionId,
                           @Param("stageId") Long stageId,
                           @Param("groupScopeKey") Long groupScopeKey);

    List<BasketStanding> selectByStageScope(@Param("divisionId") Long divisionId,
                                            @Param("stageId") Long stageId,
                                            @Param("groupScopeKey") Long groupScopeKey);

    List<BasketStanding> selectByDivisionStageGroup(@Param("divisionId") Long divisionId,
                                                    @Param("stageId") Long stageId,
                                                    @Param("groupId") Long groupId);

    List<BasketStanding> selectByStageId(@Param("stageId") Long stageId);

    int countManualPendingByStageId(@Param("stageId") Long stageId);
    int insert(BasketStanding entity);
    int updateById(BasketStanding entity);
    int softDelete(@Param("id") Long id);
}
