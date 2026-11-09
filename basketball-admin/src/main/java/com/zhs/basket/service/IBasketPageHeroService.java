package com.zhs.basket.service;

import com.zhs.basket.req.PageHeroSaveReq;
import com.zhs.basket.resp.PageHeroResp;

import java.util.List;

public interface IBasketPageHeroService {
    PageHeroResp get(String pageKey, Long seasonId);

    List<PageHeroResp> listGeneric();

    PageHeroResp save(PageHeroSaveReq req);

    String publish(PageHeroSaveReq req);
}
