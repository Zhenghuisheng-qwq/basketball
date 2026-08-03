package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketMatch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketMatch
 */
public interface BasketV4MatchMapper {
    BasketMatch selectById(@Param("id") Long id);
    List<BasketMatch> selectList(BasketMatch query);
    int insert(BasketMatch entity);
    int updateById(BasketMatch entity);
    int softDelete(@Param("id") Long id);
}
