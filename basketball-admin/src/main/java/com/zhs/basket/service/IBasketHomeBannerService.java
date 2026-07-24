package com.zhs.basket.service;

import com.zhs.basket.req.HomeBannerAddReq;
import com.zhs.basket.req.HomeBannerEditReq;
import com.zhs.basket.req.HomeBannerQueryReq;
import com.zhs.basket.resp.HomeBannerListResp;

import java.util.List;

public interface IBasketHomeBannerService {
    List<HomeBannerListResp> selectList(HomeBannerQueryReq query);

    HomeBannerListResp selectById(Long id);

    Long add(HomeBannerAddReq req);

    void edit(Long id, HomeBannerEditReq req);

    void publish(Long id);

    void offline(Long id);

    void remove(Long id);
}
