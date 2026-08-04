package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivisionGroupingBatch;
import com.zhs.basket.query.v4.BasketDivisionGroupingBatchQuery;
import com.zhs.basket.vo.v4.BasketDivisionGroupingBatchListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDivisionGroupingBatchMapper {
    BasketDivisionGroupingBatch selectById(@Param("id") Long id);

    BasketDivisionGroupingBatch selectActiveByDivisionAndStage(@Param("divisionId") Long divisionId,
                                                                @Param("stageId") Long stageId);

    int selectNextBatchVersion(@Param("divisionId") Long divisionId, @Param("stageId") Long stageId);

    List<BasketDivisionGroupingBatchListVo> selectVoList(BasketDivisionGroupingBatchQuery query);

    int insert(BasketDivisionGroupingBatch entity);

    int updateById(BasketDivisionGroupingBatch entity);

    int softDelete(@Param("id") Long id);
}
