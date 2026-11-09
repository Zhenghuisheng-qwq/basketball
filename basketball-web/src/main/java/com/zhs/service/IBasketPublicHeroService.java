package com.zhs.service;

import com.zhs.response.publicapi.PublicPageHeroResp;

public interface IBasketPublicHeroService {
    PublicPageHeroResp getPublished(String pageKey, Long seasonId);
}
