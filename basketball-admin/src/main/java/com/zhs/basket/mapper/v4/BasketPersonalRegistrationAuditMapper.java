package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketPersonalRegistrationAudit;
import com.zhs.basket.vo.v4.BasketPersonalRegistrationAuditVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketPersonalRegistrationAuditMapper {
    int insert(BasketPersonalRegistrationAudit entity);

    List<BasketPersonalRegistrationAuditVo> selectVoByPersonalRegistrationId(@Param("personalRegistrationId") Long personalRegistrationId);
}
