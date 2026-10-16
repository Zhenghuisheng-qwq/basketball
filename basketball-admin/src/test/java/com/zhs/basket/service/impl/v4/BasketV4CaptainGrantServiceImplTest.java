package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.dto.v4.BasketCaptainGrantAdjustReq;
import com.zhs.basket.dto.v4.BasketCaptainGrantCreateReq;
import com.zhs.basket.dto.v4.BasketCaptainGrantSyncReq;
import com.zhs.basket.enums.CaptainGrantLogActionTypeEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.enums.v4.CaptainGrantValidToModeEnum;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.SeasonStatusEnum;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.support.CaptainGrantLogSupport;
import com.zhs.basket.support.v4.BasketV4ParticipationSupport;
import com.zhs.basket.support.v4.CaptainGrantEventSyncSupport;
import com.zhs.basket.support.v4.CaptainGrantRuntimeSupport;
import com.zhs.basket.vo.v4.BasketCaptainGrantSyncPreviewVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.web.controller.basket.v4.BasketV4CaptainGrantController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketV4CaptainGrantServiceImplTest {

    @InjectMocks
    private BasketV4CaptainGrantServiceImpl service;

    @Mock
    private BasketCaptainGrantMapper captainGrantMapper;
    @Mock
    private BasketDivisionMapper divisionMapper;
    @Mock
    private BasketSeasonMapper seasonMapper;
    @Mock
    private BasketV4ParticipationSupport participationSupport;
    @Mock
    private CaptainGrantRuntimeSupport grantRuntimeSupport;
    @Mock
    private CaptainGrantLogSupport grantLogSupport;
    @Mock
    private CaptainGrantEventSyncSupport grantEventSyncSupport;

    @Test
    void newGrantDefaultsToAutoEvent() {
        Date end = new Date(System.currentTimeMillis() + 10L * 24 * 60 * 60 * 1000);
        BasketDivision division = division();
        BasketSeason season = season();
        BasketPlayerProfile player = new BasketPlayerProfile();
        player.setId(8L);
        player.setMemberId(9L);
        BasketCaptainGrantCreateReq req = new BasketCaptainGrantCreateReq();
        req.setDivisionId(7L);
        req.setPlayerId(8L);
        when(divisionMapper.selectById(7L)).thenReturn(division);
        when(seasonMapper.selectById(3L)).thenReturn(season);
        when(participationSupport.requireCertifiedPlayer(8L)).thenReturn(player);
        when(captainGrantMapper.selectActiveByDivisionPlayer(7L, 8L)).thenReturn(null);
        when(grantRuntimeSupport.resolveValidTo(7L)).thenReturn(end);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(1L);
            service.create(req);
        }

        ArgumentCaptor<BasketCaptainGrant> captor = ArgumentCaptor.forClass(BasketCaptainGrant.class);
        verify(captainGrantMapper).insert(captor.capture());
        assertEquals(CaptainGrantValidToModeEnum.AUTO_EVENT.getCode(), captor.getValue().getValidToMode());
        assertEquals(end, captor.getValue().getValidTo());
        verify(grantLogSupport).writeGrantLog(any(), eq(CaptainGrantLogActionTypeEnum.GRANT), any(), any(), any(), any(), eq("ADMIN"));
    }

    @Test
    void manualShortenAndExtendStayWithinEventEnd() {
        Date start = new Date(1760140800000L);
        Date mid = new Date(1760745600000L);
        Date end = new Date(1761782400000L);
        BasketCaptainGrant grant = activeGrant(start, mid, null);
        when(captainGrantMapper.selectById(1L)).thenReturn(grant);
        when(divisionMapper.selectById(7L)).thenReturn(division());
        when(seasonMapper.selectById(3L)).thenReturn(season());
        when(grantEventSyncSupport.requireEventEnd(7L)).thenReturn(end);

        BasketCaptainGrantAdjustReq shorten = new BasketCaptainGrantAdjustReq();
        shorten.setValidToMode("CUSTOM");
        shorten.setValidTo(new Date(start.getTime() + 86_400_000L));
        service.adjustValidTo(1L, shorten);
        verify(grantEventSyncSupport).applyWindow(eq(grant), eq(shorten.getValidTo()), eq("CUSTOM"),
                eq("手动指定授权截止时间"), eq(CaptainGrantLogActionTypeEnum.ADJUST_VALID_TO));

        BasketCaptainGrantAdjustReq extend = new BasketCaptainGrantAdjustReq();
        extend.setValidToMode("CUSTOM");
        extend.setValidTo(end);
        service.adjustValidTo(1L, extend);
        verify(grantEventSyncSupport).applyWindow(eq(grant), eq(end), eq("CUSTOM"),
                eq("手动指定授权截止时间"), eq(CaptainGrantLogActionTypeEnum.ADJUST_VALID_TO));
    }

    @Test
    void manualValidToAfterEventEndIsRejected() {
        Date start = new Date(1760140800000L);
        Date end = new Date(1761782400000L);
        when(captainGrantMapper.selectById(1L)).thenReturn(activeGrant(start, end, "AUTO_EVENT"));
        when(divisionMapper.selectById(7L)).thenReturn(division());
        when(seasonMapper.selectById(3L)).thenReturn(season());
        when(grantEventSyncSupport.requireEventEnd(7L)).thenReturn(end);
        BasketCaptainGrantAdjustReq req = new BasketCaptainGrantAdjustReq();
        req.setValidToMode("CUSTOM");
        req.setValidTo(new Date(end.getTime() + 86_400_000L));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.adjustValidTo(1L, req));
        assertEquals("授权截止时间不能晚于赛事结束时间", ex.getMessage());
    }

    @Test
    void restoreAutoEventUsesCurrentEventEnd() {
        Date start = new Date(1760140800000L);
        Date end = new Date(1761782400000L);
        BasketCaptainGrant grant = activeGrant(start, start, "CUSTOM");
        when(captainGrantMapper.selectById(1L)).thenReturn(grant);
        when(divisionMapper.selectById(7L)).thenReturn(division());
        when(seasonMapper.selectById(3L)).thenReturn(season());
        when(grantEventSyncSupport.requireEventEnd(7L)).thenReturn(end);
        BasketCaptainGrantAdjustReq req = new BasketCaptainGrantAdjustReq();
        req.setValidToMode("AUTO_EVENT");

        service.adjustValidTo(1L, req);

        verify(grantEventSyncSupport).applyWindow(eq(grant), eq(end), eq("AUTO_EVENT"),
                eq("恢复跟随赛事结束时间"), eq(CaptainGrantLogActionTypeEnum.ADJUST_VALID_TO));
    }

    @Test
    void revokedGrantIsNotAdjusted() {
        BasketCaptainGrant grant = activeGrant(new Date(), new Date(), null);
        grant.setGrantStatus(CaptainGrantStatusEnum.REVOKED.getCode());
        when(captainGrantMapper.selectById(1L)).thenReturn(grant);
        doThrow(new ServiceException("已撤销授权不能通过改期恢复")).when(grantEventSyncSupport).assertActiveGrant(grant);
        BasketCaptainGrantAdjustReq req = new BasketCaptainGrantAdjustReq();
        req.setValidToMode("AUTO_EVENT");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.adjustValidTo(1L, req));
        assertEquals("已撤销授权不能通过改期恢复", ex.getMessage());
    }

    @Test
    void legacyGrantCanBeExplicitlySynced() {
        Date divisionEnd = new Date(1760745600000L);
        BasketCaptainGrant grant = activeGrant(divisionEnd, divisionEnd, null);
        BasketDivision division = division();
        when(captainGrantMapper.selectById(1L)).thenReturn(grant);
        when(divisionMapper.selectById(7L)).thenReturn(division);
        when(seasonMapper.selectById(3L)).thenReturn(season());
        when(grantEventSyncSupport.requireEventEnd(7L)).thenReturn(divisionEnd);
        when(grantEventSyncSupport.eventEndSource(division)).thenReturn("DIVISION");
        BasketCaptainGrantSyncReq req = new BasketCaptainGrantSyncReq();
        req.setIds(List.of(1L));

        BasketCaptainGrantSyncPreviewVo preview = service.previewSync(req);
        assertTrue(preview.getItems().get(0).isApplicable());
        assertEquals(divisionEnd, preview.getItems().get(0).getTargetValidTo());
        assertTrue(preview.getItems().get(0).getMessage().contains("请先修改赛区比赛结束时间"));

        service.syncEventTime(req);
        verify(grantEventSyncSupport).applyWindow(eq(grant), eq(divisionEnd), eq("AUTO_EVENT"),
                eq("管理员同步赛事结束时间"), eq(CaptainGrantLogActionTypeEnum.SYNC_EVENT_TIME));
    }

    @Test
    void editEndpointRequiresEditPermission() throws Exception {
        Method method = BasketV4CaptainGrantController.class.getMethod(
                "adjustValidTo", Long.class, BasketCaptainGrantAdjustReq.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("basket:v4CaptainGrant:edit"));
        assertFalse(preAuthorize.value().contains("basket:v4CaptainGrant:add"));
    }

    private BasketCaptainGrant activeGrant(Date from, Date to, String mode) {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setId(1L);
        grant.setDivisionId(7L);
        grant.setSeasonId(3L);
        grant.setPlayerId(8L);
        grant.setMemberId(9L);
        grant.setGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        grant.setValidFrom(from);
        grant.setValidTo(to);
        grant.setValidToMode(mode);
        return grant;
    }

    private BasketDivision division() {
        BasketDivision division = new BasketDivision();
        division.setId(7L);
        division.setSeasonId(3L);
        division.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        division.setDivisionStatus(DivisionStatusEnum.REGISTRATION_OPEN.getCode());
        division.setMatchEndTime(new Date(1760745600000L));
        return division;
    }

    private BasketSeason season() {
        BasketSeason season = new BasketSeason();
        season.setId(3L);
        season.setSeasonStatus(SeasonStatusEnum.NOT_STARTED.getCode());
        season.setSeasonEndTime(new Date(1761782400000L));
        return season;
    }
}
