package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketDisciplineSuspensionLog;
import org.apache.ibatis.annotations.Param;

public interface BasketDisciplineSuspensionLogMapper {
    int insert(BasketDisciplineSuspensionLog row);

    int countByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}
