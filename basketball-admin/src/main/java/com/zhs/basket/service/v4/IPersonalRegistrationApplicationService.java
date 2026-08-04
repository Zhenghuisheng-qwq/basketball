package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketPersonalRegistrationRejectReq;
import com.zhs.basket.query.v4.BasketPersonalRegistrationQuery;
import com.zhs.basket.vo.v4.BasketPersonalRegistrationDetailVo;
import com.zhs.basket.vo.v4.BasketPersonalRegistrationListVo;

import java.util.List;

public interface IPersonalRegistrationApplicationService {
    List<BasketPersonalRegistrationListVo> selectList(BasketPersonalRegistrationQuery query);

    BasketPersonalRegistrationDetailVo selectDetail(Long personalRegistrationId);

    void approve(Long personalRegistrationId);

    void reject(Long personalRegistrationId, BasketPersonalRegistrationRejectReq req);
}
