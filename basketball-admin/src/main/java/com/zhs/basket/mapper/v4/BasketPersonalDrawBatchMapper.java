package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketPersonalDrawBatch;
import com.zhs.basket.query.v4.BasketPersonalDrawBatchQuery;
import com.zhs.basket.vo.v4.BasketPersonalDrawBatchListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPersonalDrawBatchMapper {
    BasketPersonalDrawBatch selectById(@Param("id") Long id);

    BasketPersonalDrawBatch selectActiveByDivision(@Param("divisionId") Long divisionId);

    int selectNextBatchVersion(@Param("divisionId") Long divisionId);

    List<BasketPersonalDrawBatchListVo> selectVoList(BasketPersonalDrawBatchQuery query);

    int insert(BasketPersonalDrawBatch entity);

    int updateById(BasketPersonalDrawBatch entity);

    int softDelete(@Param("id") Long id);

    int clearActiveUniqueKey(@Param("id") Long id);

    int casLock(@Param("id") Long id, @Param("rosterVersionId") Long rosterVersionId, @Param("operatorId") Long operatorId);
}
