package com.zhs.service.impl;

import com.zhs.dao.BasketPageHeroDao;
import com.zhs.model.BasketPageHero;
import com.zhs.response.publicapi.PublicPageHeroResp;
import com.zhs.service.IBasketPublicHeroService;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class BasketPublicHeroServiceImpl implements IBasketPublicHeroService {

    private static final String EVENT_SEASON = "EVENT_SEASON";
    private static final Set<String> PAGE_KEYS = Set.of(EVENT_SEASON, "DATA", "HALL", "ANNOUNCEMENT", "PARTNER");

    @Resource
    private BasketPageHeroDao pageHeroDao;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public PublicPageHeroResp getPublished(String pageKey, Long seasonId) {
        String key = pageKey == null ? "" : pageKey.trim().toUpperCase();
        long season = !EVENT_SEASON.equals(key) || seasonId == null || seasonId <= 0 ? 0L : seasonId;
        if (!PAGE_KEYS.contains(key) || (EVENT_SEASON.equals(key) && season <= 0)) {
            return empty(key, null);
        }
        String cacheKey = "hero:" + key + ":" + season;
        return publicSupport.cacheGetOrLoad(cacheKey, () -> load(key, season));
    }

    private PublicPageHeroResp load(String pageKey, long seasonId) {
        BasketPageHero row = pageHeroDao.selectPublished(pageKey, seasonId);
        if (row == null || row.getImageFileId() == null) {
            return empty(pageKey, seasonId == 0 ? null : seasonId);
        }
        Set<Long> fileIds = new HashSet<>();
        fileIds.add(row.getImageFileId());
        if (row.getMobileImageFileId() != null) {
            fileIds.add(row.getMobileImageFileId());
        }
        Map<Long, String> urls = publicSupport.loadAccessUrlMap(fileIds);
        String imageUrl = urls.get(row.getImageFileId());
        if (!StringUtils.hasText(imageUrl)) {
            return empty(pageKey, seasonId == 0 ? null : seasonId);
        }
        PublicPageHeroResp resp = new PublicPageHeroResp();
        resp.setConfigured(true);
        resp.setPageKey(pageKey);
        resp.setSeasonId(seasonId == 0 ? null : seasonId);
        resp.setMode(StringUtils.hasText(row.getMode()) ? row.getMode() : "BACKGROUND_TEXT");
        resp.setImageUrl(imageUrl);
        resp.setMobileImageUrl(row.getMobileImageFileId() == null ? null : urls.get(row.getMobileImageFileId()));
        resp.setFocusX(row.getFocusX() == null ? 50 : row.getFocusX());
        resp.setFocusY(row.getFocusY() == null ? 50 : row.getFocusY());
        resp.setMaskOpacity(row.getMaskOpacity() == null ? 40 : row.getMaskOpacity());
        resp.setAltText(row.getAltText());
        return resp;
    }

    private PublicPageHeroResp empty(String pageKey, Long seasonId) {
        PublicPageHeroResp resp = new PublicPageHeroResp();
        resp.setConfigured(false);
        resp.setPageKey(pageKey);
        resp.setSeasonId(seasonId);
        return resp;
    }
}
