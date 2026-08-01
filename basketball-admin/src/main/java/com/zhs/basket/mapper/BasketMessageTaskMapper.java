package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMessageTask;
import org.apache.ibatis.annotations.Param;

public interface BasketMessageTaskMapper {
    int insert(BasketMessageTask record);

    int countByBizTypeAndBizId(@Param("bizType") String bizType, @Param("bizId") Long bizId);
}
