package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketRegistrationPlayer
 */
public interface BasketRegistrationPlayerMapper {
    BasketRegistrationPlayer selectById(@Param("id") Long id);
    List<BasketRegistrationPlayer> selectList(BasketRegistrationPlayer query);
    int insert(BasketRegistrationPlayer entity);
    int updateById(BasketRegistrationPlayer entity);
    int softDelete(@Param("id") Long id);
}
