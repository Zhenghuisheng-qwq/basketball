package com.zhs.service;

import com.zhs.response.publicapi.PublicSiteThemeResp;

public interface IBasketPublicThemeService {
    PublicSiteThemeResp getPublished(String scope, Long seasonId);
}
