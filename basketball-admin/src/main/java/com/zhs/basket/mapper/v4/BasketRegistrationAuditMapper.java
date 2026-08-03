package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketRegistrationAudit;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * CNPA 赛事域 V4 Mapper：BasketRegistrationAudit
 */
public interface BasketRegistrationAuditMapper {
    BasketRegistrationAudit selectById(@Param("id") Long id);
    List<BasketRegistrationAudit> selectList(BasketRegistrationAudit query);
    int insert(BasketRegistrationAudit entity);
    int updateById(BasketRegistrationAudit entity);
    int softDelete(@Param("id") Long id);
}
