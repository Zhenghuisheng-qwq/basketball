package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketDivision
 */
public interface BasketDivisionMapper {
    BasketDivision selectById(@Param("id") Long id);
    List<BasketDivision> selectList(BasketDivision query);
    int insert(BasketDivision entity);
    int updateById(BasketDivision entity);
    int softDelete(@Param("id") Long id);
}
