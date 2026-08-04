package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivisionGroup;
import com.zhs.basket.vo.v4.BasketDivisionGroupVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDivisionGroupMapper {
    BasketDivisionGroup selectById(@Param("id") Long id);

    List<BasketDivisionGroup> selectByBatchId(@Param("groupingBatchId") Long groupingBatchId);

    List<BasketDivisionGroupVo> selectVoByBatchId(@Param("groupingBatchId") Long groupingBatchId);

    int countByBatchId(@Param("groupingBatchId") Long groupingBatchId);

    int insert(BasketDivisionGroup entity);

    int updateById(BasketDivisionGroup entity);

    int softDelete(@Param("id") Long id);
}
