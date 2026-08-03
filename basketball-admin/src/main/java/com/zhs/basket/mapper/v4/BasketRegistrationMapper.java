package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketRegistration;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketRegistration
 */
public interface BasketRegistrationMapper {
    BasketRegistration selectById(@Param("id") Long id);
    List<BasketRegistration> selectList(BasketRegistration query);
    int insert(BasketRegistration entity);
    int updateById(BasketRegistration entity);
    int softDelete(@Param("id") Long id);
}
