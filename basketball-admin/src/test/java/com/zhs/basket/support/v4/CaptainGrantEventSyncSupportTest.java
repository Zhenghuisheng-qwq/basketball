package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.enums.CaptainGrantLogActionTypeEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.enums.v4.CaptainGrantValidToModeEnum;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.SeasonStatusEnum;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.support.CaptainGrantLogSupport;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaptainGrantEventSyncSupportTest {

    @InjectMocks
    private CaptainGrantEventSyncSupport syncSupport;

    @Mock
    private BasketCaptainGrantMapper captainGrantMapper;
    @Mock
    private BasketDivisionMapper divisionMapper;
    @Mock
    private BasketSeasonMapper seasonMapper;
    @Mock
    private CaptainGrantRuntimeSupport grantRuntimeSupport;
    @Mock
    private CaptainGrantLogSupport grantLogSupport;

    private Date oct18;
    private Date oct30;

    @BeforeEach
    void setUp() {
        oct18 = new Date(1760745600000L);
        oct30 = new Date(1761782400000L);
    }

    @Test
    void divisionEndChangeSyncsOnlyReturnedAutoEventGrants() {
        BasketCaptainGrant grant = activeGrant(oct18, CaptainGrantValidToModeEnum.AUTO_EVENT.getCode());
        when(grantRuntimeSupport.resolveValidTo(7L)).thenReturn(oct30);
        when(captainGrantMapper.selectAutoEventByDivisionId(7L)).thenReturn(List.of(grant));
        when(captainGrantMapper.updateValidWindow(1L, oct30, "AUTO_EVENT", oct18, "AUTO_EVENT")).thenReturn(1);

        syncSupport.onDivisionMatchEndChanged(7L, oct18, oct30);

        verify(captainGrantMapper).updateValidWindow(1L, oct30, "AUTO_EVENT", oct18, "AUTO_EVENT");
        verify(grantLogSupport).writeGrantLog(eq(grant), eq(CaptainGrantLogActionTypeEnum.SYNC_EVENT_TIME),
                eq(1), eq(1), eq("赛区比赛结束时间变更"), any(), eq("ADMIN"));
    }

    @Test
    void unchangedDivisionEndDoesNotSync() {
        syncSupport.onDivisionMatchEndChanged(7L, oct18, new Date(oct18.getTime()));
        verify(captainGrantMapper, never()).selectAutoEventByDivisionId(any());
    }

    @Test
    void seasonEndChangeSyncsOnlySeasonDependentGrants() {
        BasketSeason season = new BasketSeason();
        season.setSeasonEndTime(oct30);
        BasketCaptainGrant grant = activeGrant(oct18, CaptainGrantValidToModeEnum.AUTO_EVENT.getCode());
        when(seasonMapper.selectById(3L)).thenReturn(season);
        when(captainGrantMapper.selectAutoEventDependingOnSeason(3L)).thenReturn(List.of(grant));
        when(captainGrantMapper.updateValidWindow(1L, oct30, "AUTO_EVENT", oct18, "AUTO_EVENT")).thenReturn(1);

        syncSupport.onSeasonEndChanged(3L, oct18, oct30);

        verify(captainGrantMapper).selectAutoEventDependingOnSeason(3L);
        verify(captainGrantMapper, never()).selectAutoEventByDivisionId(any());
        verify(captainGrantMapper).updateValidWindow(1L, oct30, "AUTO_EVENT", oct18, "AUTO_EVENT");
    }

    @Test
    void registrationWindowChangeIsNotASeasonEndChange() {
        syncSupport.onSeasonEndChanged(3L, oct30, oct30);
        verify(seasonMapper, never()).selectById(any());
    }

    @Test
    void customGrantIsNotSelectedByAutoQuery() {
        when(grantRuntimeSupport.resolveValidTo(7L)).thenReturn(oct30);
        when(captainGrantMapper.selectAutoEventByDivisionId(7L)).thenReturn(Collections.emptyList());

        syncSupport.onDivisionMatchEndChanged(7L, oct18, oct30);

        verify(captainGrantMapper, never()).updateValidWindow(any(), any(), any(), any(), any());
    }

    @Test
    void activeButAlreadyExpiredIsNotRevivedByEventExtension() {
        Date past = new Date(System.currentTimeMillis() - 86_400_000L);
        Date future = new Date(System.currentTimeMillis() + 10L * 86_400_000L);
        BasketCaptainGrant grant = activeGrant(past, CaptainGrantValidToModeEnum.AUTO_EVENT.getCode());
        when(grantRuntimeSupport.resolveValidTo(7L)).thenReturn(future);
        when(captainGrantMapper.selectAutoEventByDivisionId(7L)).thenReturn(List.of(grant));

        syncSupport.onDivisionMatchEndChanged(7L, past, future);

        verify(captainGrantMapper, never()).updateValidWindow(any(), any(), any(), any(), any());
    }

    @Test
    void sameTargetSkipsWrite() {
        BasketCaptainGrant grant = activeGrant(oct30, CaptainGrantValidToModeEnum.AUTO_EVENT.getCode());
        when(grantRuntimeSupport.resolveValidTo(7L)).thenReturn(oct30);
        when(captainGrantMapper.selectAutoEventByDivisionId(7L)).thenReturn(List.of(grant));

        syncSupport.onDivisionMatchEndChanged(7L, oct18, oct30);

        verify(captainGrantMapper, never()).updateValidWindow(any(), any(), any(), any(), any());
        verify(grantLogSupport, never()).writeGrantLog(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void lostUpdateThrowsWithoutLoggingSuccess() {
        BasketCaptainGrant grant = activeGrant(oct18, CaptainGrantValidToModeEnum.AUTO_EVENT.getCode());
        when(captainGrantMapper.updateValidWindow(1L, oct30, "CUSTOM", oct18, "AUTO_EVENT")).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> syncSupport.applyWindow(
                grant, oct30, "CUSTOM", "手动", CaptainGrantLogActionTypeEnum.ADJUST_VALID_TO));

        assertEquals("授权有效期已变化，请刷新后重试", ex.getMessage());
        verify(grantLogSupport, never()).writeGrantLog(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void endedSeasonCannotExtend() {
        BasketSeason season = new BasketSeason();
        season.setSeasonStatus(SeasonStatusEnum.ARCHIVED.getCode());
        ServiceException ex = assertThrows(ServiceException.class,
                () -> syncSupport.assertEventStillOpen(openDivision(), season));
        assertEquals("赛事已结束或归档，不能延长队长授权", ex.getMessage());
    }

    @Test
    void revokedAndExpiredCannotBeAdjusted() {
        BasketCaptainGrant revoked = activeGrant(oct18, null);
        revoked.setGrantStatus(CaptainGrantStatusEnum.REVOKED.getCode());
        assertEquals("已撤销授权不能通过改期恢复",
                assertThrows(ServiceException.class, () -> syncSupport.assertActiveGrant(revoked)).getMessage());

        BasketCaptainGrant expired = activeGrant(oct18, null);
        expired.setGrantStatus(CaptainGrantStatusEnum.EXPIRED.getCode());
        assertEquals("已过期授权不能通过改期恢复，请重新授权",
                assertThrows(ServiceException.class, () -> syncSupport.assertActiveGrant(expired)).getMessage());
    }

    @Test
    void divisionSourceWinsOverSeason() {
        BasketDivision division = openDivision();
        division.setMatchEndTime(oct18);
        assertEquals("DIVISION", syncSupport.eventEndSource(division));
        division.setMatchEndTime(null);
        assertEquals("SEASON", syncSupport.eventEndSource(division));
    }

    private BasketDivision openDivision() {
        BasketDivision division = new BasketDivision();
        division.setId(7L);
        division.setDivisionStatus(DivisionStatusEnum.REGISTRATION_OPEN.getCode());
        return division;
    }

    private BasketCaptainGrant activeGrant(Date validTo, String mode) {
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setId(1L);
        grant.setDivisionId(7L);
        grant.setSeasonId(3L);
        grant.setGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        grant.setValidTo(validTo);
        grant.setValidToMode(mode);
        grant.setMemberId(9L);
        grant.setPlayerId(8L);
        return grant;
    }
}
