package com.zhs.service.impl;

import com.zhs.dao.BasketSiteSocialLinkDao;
import com.zhs.model.BasketSiteSocialLink;
import com.zhs.response.publicapi.PublicSocialLinkResp;
import com.zhs.service.support.BasketPublicSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketPublicSocialLinkServiceImplTest {

    @Mock
    private BasketSiteSocialLinkDao socialLinkDao;
    @Mock
    private BasketPublicSupport publicSupport;

    @InjectMocks
    private BasketPublicSocialLinkServiceImpl service;

    @Test
    void sanitizesUnsafeUrlAndKeepsEmpty() {
        when(publicSupport.cacheGetOrLoad(anyString(), any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<?> loader = invocation.getArgument(1);
            return loader.get();
        });
        BasketSiteSocialLink kook = link(1L, "KOOK", "https://kook.top/cnpa", 10);
        BasketSiteSocialLink wechat = link(2L, "WECHAT", "", 5);
        BasketSiteSocialLink bili = link(3L, "BILIBILI", "javascript:alert(1)", 30);
        when(socialLinkDao.selectList(any())).thenReturn(List.of(wechat, kook, bili));

        List<PublicSocialLinkResp> result = service.listEnabled();

        assertEquals(3, result.size());
        assertEquals("WECHAT", result.get(0).getPlatformCode());
        assertEquals("", result.get(0).getJumpUrl());
        assertEquals("https://kook.top/cnpa", result.get(1).getJumpUrl());
        assertEquals("", result.get(2).getJumpUrl());
    }

    private BasketSiteSocialLink link(Long id, String platform, String url, int sort) {
        BasketSiteSocialLink item = new BasketSiteSocialLink();
        item.setId(id);
        item.setPlatformCode(platform);
        item.setTitle(platform);
        item.setSubtitle("desc");
        item.setButtonText("go");
        item.setJumpUrl(url);
        item.setSortOrder(sort);
        item.setStatus(1);
        item.setIsDelete(0);
        return item;
    }
}
