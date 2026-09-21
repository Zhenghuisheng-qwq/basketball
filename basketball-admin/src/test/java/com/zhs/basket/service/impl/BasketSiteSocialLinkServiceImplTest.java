package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketSiteSocialLink;
import com.zhs.basket.mapper.BasketSiteSocialLinkMapper;
import com.zhs.basket.req.SiteSocialLinkEditReq;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketSiteSocialLinkServiceImplTest {

    @Mock
    private BasketSiteSocialLinkMapper socialLinkMapper;
    @Mock
    private BasketPublicCacheCleaner publicCacheCleaner;

    @InjectMocks
    private BasketSiteSocialLinkServiceImpl service;

    @Test
    void editRejectsJavascriptUrl() {
        when(socialLinkMapper.selectById(1L)).thenReturn(existing());
        SiteSocialLinkEditReq req = validReq();
        req.setJumpUrl("javascript:alert(1)");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.edit(1L, req));
        assertTrueContains(ex.getMessage(), "javascript");
        verify(socialLinkMapper, never()).updateById(any());
    }

    @Test
    void editAllowsEmptyUrl() {
        when(socialLinkMapper.selectById(1L)).thenReturn(existing());
        when(socialLinkMapper.updateById(any())).thenReturn(1);
        SiteSocialLinkEditReq req = validReq();
        req.setJumpUrl("");

        service.edit(1L, req);

        verify(socialLinkMapper).updateById(org.mockito.ArgumentMatchers.argThat(link -> {
            assertNull(link.getJumpUrl());
            assertEquals(Integer.valueOf(1), link.getStatus());
            return true;
        }));
        verify(publicCacheCleaner).clearAfterCommit();
    }

    @Test
    void editRejectsInvalidStatus() {
        when(socialLinkMapper.selectById(1L)).thenReturn(existing());
        SiteSocialLinkEditReq req = validReq();
        req.setStatus(2);
        assertThrows(ServiceException.class, () -> service.edit(1L, req));
        verify(socialLinkMapper, never()).updateById(any());
    }

    private BasketSiteSocialLink existing() {
        BasketSiteSocialLink link = new BasketSiteSocialLink();
        link.setId(1L);
        link.setPlatformCode("KOOK");
        link.setTitle("加入 KOOK");
        link.setSubtitle("官方赛事社区");
        link.setButtonText("加入社区 →");
        link.setSortOrder(10);
        link.setStatus(1);
        link.setIsDelete(0);
        return link;
    }

    private SiteSocialLinkEditReq validReq() {
        SiteSocialLinkEditReq req = new SiteSocialLinkEditReq();
        req.setTitle("加入 KOOK");
        req.setSubtitle("官方赛事社区");
        req.setButtonText("加入社区 →");
        req.setJumpUrl("https://kook.top/cnpa");
        req.setSortOrder(10);
        req.setStatus(1);
        return req;
    }

    private void assertTrueContains(String actual, String expected) {
        if (actual == null || !actual.contains(expected)) {
            throw new AssertionError("expected message containing '" + expected + "' but was '" + actual + "'");
        }
    }
}
