package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketStandingRankReason;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketStandingRankReasonMapper {

    List<BasketStandingRankReason> selectByStandingId(@Param("standingId") Long standingId);

    List<BasketStandingRankReason> selectByStandingIds(@Param("standingIds") List<Long> standingIds);

    int softDeleteByStageScope(@Param("divisionId") Long divisionId,
                               @Param("stageId") Long stageId,
                               @Param("groupScopeKey") Long groupScopeKey);

    int insert(BasketStandingRankReason entity);

    int batchInsert(@Param("list") List<BasketStandingRankReason> list);
}
