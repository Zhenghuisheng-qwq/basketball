package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDivisionStageMapper {
    BasketDivisionStage selectById(@Param("id") Long id);

    List<BasketDivisionStage> selectBySnapshotId(@Param("snapshotId") Long snapshotId);

    int insert(BasketDivisionStage entity);

    int updateById(BasketDivisionStage entity);

    int softDeleteById(@Param("id") Long id);

    int softDeleteBySnapshotId(@Param("snapshotId") Long snapshotId);

    int updateStageOrder(@Param("id") Long id, @Param("stageOrder") Integer stageOrder);

    int updateExecutionStatus(BasketDivisionStage entity);

    List<BasketDivisionStage> selectByDivisionId(@Param("divisionId") Long divisionId);

    BasketDivisionStage selectFirstByDivisionId(@Param("divisionId") Long divisionId);

    BasketDivisionStage selectBySnapshotIdAndStageCode(@Param("snapshotId") Long snapshotId,
                                                       @Param("stageCode") String stageCode);
}
