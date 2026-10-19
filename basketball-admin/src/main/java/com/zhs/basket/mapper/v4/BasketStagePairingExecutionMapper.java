package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketStagePairingExecution;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketStagePairingExecutionMapper {

    BasketStagePairingExecution selectById(@Param("id") Long id);

    BasketStagePairingExecution selectCurrentByStageId(@Param("stageId") Long stageId);

    BasketStagePairingExecution selectCurrentByStageIdForUpdate(@Param("stageId") Long stageId);

    List<BasketStagePairingExecution> selectByStageId(@Param("stageId") Long stageId);

    Integer selectMaxExecutionNo(@Param("stageId") Long stageId);

    int insert(BasketStagePairingExecution entity);

    int updateById(BasketStagePairingExecution entity);

    int clearCurrentByStageId(@Param("stageId") Long stageId);

    int markSuperseded(@Param("id") Long id);

    int rebindStage(@Param("id") Long id, @Param("stageId") Long stageId, @Param("sourceStageId") Long sourceStageId);
}
