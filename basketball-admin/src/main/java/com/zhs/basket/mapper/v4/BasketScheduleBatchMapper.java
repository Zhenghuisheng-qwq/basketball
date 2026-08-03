package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketScheduleBatch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketScheduleBatch
 */
public interface BasketScheduleBatchMapper {
    BasketScheduleBatch selectById(@Param("id") Long id);
    List<BasketScheduleBatch> selectList(BasketScheduleBatch query);
    int insert(BasketScheduleBatch entity);
    int updateById(BasketScheduleBatch entity);
    int softDelete(@Param("id") Long id);
}
