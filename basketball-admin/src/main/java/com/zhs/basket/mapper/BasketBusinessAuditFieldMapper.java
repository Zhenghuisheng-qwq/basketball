package com.zhs.basket.mapper;

import com.zhs.basket.domain.BasketBusinessAuditField;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketBusinessAuditFieldMapper {
    int batchInsert(@Param("list") List<BasketBusinessAuditField> list);
}
