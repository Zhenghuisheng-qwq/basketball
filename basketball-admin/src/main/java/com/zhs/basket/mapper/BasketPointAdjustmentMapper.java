package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPointAdjustment;
import org.apache.ibatis.annotations.Param;

public interface BasketPointAdjustmentMapper {
    int insert(BasketPointAdjustment row);

    BasketPointAdjustment selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    java.util.List<BasketPointAdjustment> selectV4ByStageAndEventTeam(@Param("divisionId") Long divisionId,
                                                                      @Param("stageId") Long stageId,
                                                                      @Param("eventTeamId") Long eventTeamId);

    java.util.List<BasketPointAdjustment> selectV4SumByStage(@Param("divisionId") Long divisionId,
                                                             @Param("stageId") Long stageId);
}
