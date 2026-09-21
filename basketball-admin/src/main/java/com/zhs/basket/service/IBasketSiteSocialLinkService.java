package com.zhs.basket.service;

import com.zhs.basket.req.SiteSocialLinkEditReq;
import com.zhs.basket.resp.SiteSocialLinkListResp;

import java.util.List;

public interface IBasketSiteSocialLinkService {
    List<SiteSocialLinkListResp> selectList();

    void edit(Long id, SiteSocialLinkEditReq req);
}
