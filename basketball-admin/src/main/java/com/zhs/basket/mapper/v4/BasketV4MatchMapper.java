package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketV4Match;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketV4Match
 */
public interface BasketV4MatchMapper {
    BasketV4Match selectById(@Param("id") Long id);
    List<BasketV4Match> selectList(BasketV4Match query);
    int insert(BasketV4Match entity);
    int updateById(BasketV4Match entity);
    int softDelete(@Param("id") Long id);
}
