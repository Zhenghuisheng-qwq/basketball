package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketEventPlayer;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketEventPlayer
 */
public interface BasketEventPlayerMapper {
    BasketEventPlayer selectById(@Param("id") Long id);
    List<BasketEventPlayer> selectList(BasketEventPlayer query);
    int insert(BasketEventPlayer entity);
    int updateById(BasketEventPlayer entity);
    int softDelete(@Param("id") Long id);
}
