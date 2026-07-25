package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketPointAdjustment;
import org.apache.ibatis.annotations.Param;

public interface BasketPointAdjustmentMapper {
    int insert(BasketPointAdjustment row);

    BasketPointAdjustment selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}
