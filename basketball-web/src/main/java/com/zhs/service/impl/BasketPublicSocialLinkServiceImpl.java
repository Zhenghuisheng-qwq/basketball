package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketSiteSocialLinkDao;
import com.zhs.model.BasketSiteSocialLink;
import com.zhs.response.publicapi.PublicSocialLinkResp;
import com.zhs.service.IBasketPublicSocialLinkService;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class BasketPublicSocialLinkServiceImpl implements IBasketPublicSocialLinkService {

    private static final String CACHE_KEY = "site:social-links";
    private static final Set<String> FORBIDDEN_SCHEMES = Set.of("javascript", "data", "file", "vbscript");
    private static final int MAX_URL_LENGTH = 512;

    @Resource
    private BasketSiteSocialLinkDao socialLinkDao;
    @Resource
    private BasketPublicSupport publicSupport;

    @Override
    public List<PublicSocialLinkResp> listEnabled() {
        return publicSupport.cacheGetOrLoad(CACHE_KEY, this::loadEnabled);
    }

    private List<PublicSocialLinkResp> loadEnabled() {
        List<BasketSiteSocialLink> list = socialLinkDao.selectList(new LambdaQueryWrapper<BasketSiteSocialLink>()
                .eq(BasketSiteSocialLink::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .eq(BasketSiteSocialLink::getStatus, BasketPublicConstants.PUBLISHED)
                .orderByAsc(BasketSiteSocialLink::getSortOrder)
                .orderByAsc(BasketSiteSocialLink::getId));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<PublicSocialLinkResp> result = new ArrayList<>(list.size());
        for (BasketSiteSocialLink item : list) {
            PublicSocialLinkResp vo = new PublicSocialLinkResp();
            vo.setId(item.getId());
            vo.setPlatformCode(item.getPlatformCode());
            vo.setTitle(item.getTitle());
            vo.setTitleEn(item.getTitleEn());
            vo.setSubtitle(item.getSubtitle());
            vo.setSubtitleEn(item.getSubtitleEn());
            vo.setButtonText(item.getButtonText());
            vo.setButtonTextEn(item.getButtonTextEn());
            vo.setJumpUrl(sanitizeJumpUrl(item.getJumpUrl()));
            vo.setSortOrder(item.getSortOrder());
            result.add(vo);
        }
        return result;
    }

    private String sanitizeJumpUrl(String jumpUrl) {
        if (!StringUtils.hasText(jumpUrl)) {
            return "";
        }
        String trimmed = jumpUrl.trim();
        if (trimmed.length() > MAX_URL_LENGTH) {
            return "";
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        for (String forbidden : FORBIDDEN_SCHEMES) {
            if (lower.startsWith(forbidden + ":")) {
                return "";
            }
        }
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return "";
            }
            return trimmed;
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }
}
