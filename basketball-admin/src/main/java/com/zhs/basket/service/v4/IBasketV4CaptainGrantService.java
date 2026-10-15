package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketCaptainGrantAdjustReq;
import com.zhs.basket.dto.v4.BasketCaptainGrantCreateReq;
import com.zhs.basket.dto.v4.BasketCaptainGrantSyncReq;
import com.zhs.basket.query.v4.BasketCaptainGrantQuery;
import com.zhs.basket.vo.v4.BasketCaptainGrantListVo;
import com.zhs.basket.vo.v4.BasketCaptainGrantSyncPreviewVo;

import java.util.Date;
import java.util.List;

public interface IBasketV4CaptainGrantService {
    List<BasketCaptainGrantListVo> selectList(BasketCaptainGrantQuery query);

    Date resolveValidTo(Long divisionId);

    Long create(BasketCaptainGrantCreateReq req);

    void revoke(Long id);

    void adjustValidTo(Long id, BasketCaptainGrantAdjustReq req);

    BasketCaptainGrantSyncPreviewVo previewSync(BasketCaptainGrantSyncReq req);

    BasketCaptainGrantSyncPreviewVo syncEventTime(BasketCaptainGrantSyncReq req);
}
