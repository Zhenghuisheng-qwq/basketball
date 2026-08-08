package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketCaptainGrantCreateReq;
import com.zhs.basket.query.v4.BasketCaptainGrantQuery;
import com.zhs.basket.vo.v4.BasketCaptainGrantListVo;

import java.util.Date;
import java.util.List;

public interface IBasketV4CaptainGrantService {
    List<BasketCaptainGrantListVo> selectList(BasketCaptainGrantQuery query);

    Date resolveValidTo(Long divisionId);

    Long create(BasketCaptainGrantCreateReq req);

    void revoke(Long id);
}
