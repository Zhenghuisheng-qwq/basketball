package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketRegistrationAuditRejectReq;
import com.zhs.basket.query.v4.BasketRegistrationAuditQuery;
import com.zhs.basket.vo.v4.BasketRegistrationAuditDetailVo;
import com.zhs.basket.vo.v4.BasketRegistrationAuditListVo;

import java.util.List;

public interface IBasketV4RegistrationAuditService {
    List<BasketRegistrationAuditListVo> selectList(BasketRegistrationAuditQuery query);

    BasketRegistrationAuditDetailVo selectDetail(Long registrationId);

    void approve(Long registrationId);

    void reject(Long registrationId, BasketRegistrationAuditRejectReq req);
}
