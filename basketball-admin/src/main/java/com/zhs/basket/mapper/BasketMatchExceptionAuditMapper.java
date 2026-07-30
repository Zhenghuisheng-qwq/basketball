package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketMatchExceptionAudit;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketMatchExceptionAuditMapper {
    int insert(BasketMatchExceptionAudit audit);

    List<BasketMatchExceptionAudit> selectByApplicationId(@Param("applicationId") Long applicationId);
}
