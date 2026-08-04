package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivisionTieBreakRule;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDivisionTieBreakRuleMapper {
    List<BasketDivisionTieBreakRule> selectBySnapshotId(@Param("snapshotId") Long snapshotId);

    List<BasketDivisionTieBreakRule> selectByStageId(@Param("stageId") Long stageId);

    int insert(BasketDivisionTieBreakRule entity);

    int softDeleteBySnapshotId(@Param("snapshotId") Long snapshotId);

    int softDeleteByStageId(@Param("stageId") Long stageId);
}
