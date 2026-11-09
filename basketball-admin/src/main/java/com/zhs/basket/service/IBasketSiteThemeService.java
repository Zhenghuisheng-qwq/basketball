package com.zhs.basket.service;

import com.zhs.basket.req.SiteThemePublishReq;
import com.zhs.basket.req.SiteThemeSaveReq;
import com.zhs.basket.resp.SiteThemeImpactResp;
import com.zhs.basket.resp.SiteThemeResp;

import java.util.List;

public interface IBasketSiteThemeService {
    List<SiteThemeResp> list();

    SiteThemeResp get(Long id);

    SiteThemeResp save(SiteThemeSaveReq req);

    String publish(SiteThemePublishReq req);

    SiteThemeImpactResp impact(Long themeId, String scopeType, Long seasonId);

    Long copy(Long id);

    void offline(Long id, Integer versionNo);

    void remove(Long id);

    void unbind(String scopeType, Long seasonId);
}
