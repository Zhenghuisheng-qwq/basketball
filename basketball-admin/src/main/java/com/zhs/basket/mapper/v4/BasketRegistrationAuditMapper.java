package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketRegistrationAudit;
import com.zhs.basket.vo.v4.BasketRegistrationAuditLogVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketRegistrationAuditMapper {
    BasketRegistrationAudit selectById(@Param("id") Long id);

    List<BasketRegistrationAudit> selectList(BasketRegistrationAudit query);

    List<BasketRegistrationAuditLogVo> selectVoByRegistrationId(@Param("registrationId") Long registrationId);

    int insert(BasketRegistrationAudit entity);

    int updateById(BasketRegistrationAudit entity);

    int softDelete(@Param("id") Long id);
}
