package com.zhs.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.dao.BasketSiteThemeDao;
import com.zhs.model.BasketSiteTheme;
import com.zhs.response.publicapi.PublicSiteThemeResp;
import com.zhs.service.IBasketPublicThemeService;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class BasketPublicThemeServiceImpl implements IBasketPublicThemeService {

    private static final Set<String> SCOPES = Set.of("HOME", "SEASON", "BASE");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Resource
    private BasketSiteThemeDao themeDao;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public PublicSiteThemeResp getPublished(String scope, Long seasonId) {
        String key = scope == null ? "" : scope.trim().toUpperCase();
        if (!SCOPES.contains(key)) {
            return empty();
        }
        long season = "SEASON".equals(key) && seasonId != null && seasonId > 0 ? seasonId : 0L;
        if ("SEASON".equals(key) && season <= 0) {
            key = "BASE";
        }
        String cacheKey = "theme:" + key + ":" + ("SEASON".equals(key) ? season : 0);
        String resolvedScope = key;
        long resolvedSeason = season;
        return publicSupport.cacheGetOrLoad(cacheKey, () -> load(resolvedScope, resolvedSeason));
    }

    private PublicSiteThemeResp load(String scope, long seasonId) {
        PublicSiteThemeResp direct = read(scope, "SEASON".equals(scope) ? seasonId : 0L, scope);
        if (direct != null) {
            return direct;
        }
        if (!"BASE".equals(scope)) {
            PublicSiteThemeResp base = read("BASE", 0L, "BASE");
            if (base != null) {
                return base;
            }
        }
        return empty();
    }

    private PublicSiteThemeResp read(String scope, long scopeId, String source) {
        BasketSiteTheme theme = themeDao.selectPublished(scope, scopeId);
        if (theme == null || !StringUtils.hasText(theme.getColorsJson())) {
            return null;
        }
        Map<String, String> colors;
        try {
            colors = JSON.readValue(theme.getColorsJson(), new TypeReference<>() {});
        } catch (Exception ex) {
            return null;
        }
        Set<Long> fileIds = new HashSet<>();
        if (theme.getBgImageFileId() != null) {
            fileIds.add(theme.getBgImageFileId());
        }
        if (theme.getMobileBgImageFileId() != null) {
            fileIds.add(theme.getMobileBgImageFileId());
        }
        Map<Long, String> urls = publicSupport.loadAccessUrlMap(fileIds);
        PublicSiteThemeResp resp = new PublicSiteThemeResp();
        resp.setConfigured(true);
        resp.setSource(source);
        resp.setBgColor(theme.getBgColor());
        resp.setBgImageUrl(theme.getBgImageFileId() == null ? null : urls.get(theme.getBgImageFileId()));
        resp.setMobileBgImageUrl(theme.getMobileBgImageFileId() == null ? null : urls.get(theme.getMobileBgImageFileId()));
        resp.setBgDisplay(StringUtils.hasText(theme.getBgDisplay()) ? theme.getBgDisplay() : "TOP_CENTER");
        resp.setColors(colors);
        return resp;
    }

    private PublicSiteThemeResp empty() {
        PublicSiteThemeResp resp = new PublicSiteThemeResp();
        resp.setConfigured(false);
        resp.setSource("DEFAULT");
        return resp;
    }
}
