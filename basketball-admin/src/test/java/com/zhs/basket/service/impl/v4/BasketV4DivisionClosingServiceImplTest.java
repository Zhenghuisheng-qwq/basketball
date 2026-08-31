package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.enums.TeamStatusEnum;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.BasketTeamMemberMapper;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketTeamLifecycleClosingMapper;
import com.zhs.basket.service.v4.IBasketV4SeasonService;
import com.zhs.basket.support.CaptainGrantLogSupport;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketV4DivisionClosingServiceImplTest {

    @Mock private BasketDivisionMapper divisionMapper;
    @Mock private BasketCaptainGrantMapper captainGrantMapper;
    @Mock private BasketTeamMapper teamMapper;
    @Mock private BasketTeamMemberMapper teamMemberMapper;
    @Mock private BasketPlayerProfileMapper playerProfileMapper;
    @Mock private BasketTeamLifecycleClosingMapper lifecycleClosingMapper;
    @Mock private CaptainGrantLogSupport grantLogSupport;
    @Mock private IBasketV4SeasonService seasonService;

    @InjectMocks
    private BasketV4DivisionClosingServiceImpl closingService;

    @Test
    void finish_idempotentWhenAlreadyFinished() {
        BasketDivision division = division(118L, DivisionStatusEnum.FINISHED.getCode());
        when(divisionMapper.selectById(118L)).thenReturn(division);

        assertDoesNotThrow(() -> closingService.finishDivision(118L, "retry"));
        verify(lifecycleClosingMapper, never()).casFinishDivision(anyLong(), anyCollection(), any());
        verify(captainGrantMapper, never()).expireActiveByDivisionId(anyLong(), any());
        verify(seasonService).refreshAggregatedStatus(3L);
    }

    @Test
    void finish_rejectsDraft() {
        when(divisionMapper.selectById(118L)).thenReturn(division(118L, DivisionStatusEnum.DRAFT.getCode()));
        assertThrows(ServiceException.class, () -> closingService.finishDivision(118L, null));
    }

    @Test
    void finish_rejectsRegistrationOpen() {
        when(divisionMapper.selectById(118L))
                .thenReturn(division(118L, DivisionStatusEnum.REGISTRATION_OPEN.getCode()));
        assertThrows(ServiceException.class, () -> closingService.finishDivision(118L, null));
    }

    @Test
    void finish_happyPathArchivesAndExpires() {
        when(divisionMapper.selectById(118L))
                .thenReturn(division(118L, DivisionStatusEnum.PLAYING.getCode()));
        when(lifecycleClosingMapper.casFinishDivision(eq(118L), anyCollection(), any(Date.class))).thenReturn(1);
        BasketCaptainGrant grant = new BasketCaptainGrant();
        grant.setId(9L);
        grant.setDivisionId(118L);
        when(captainGrantMapper.selectActiveByDivisionId(118L)).thenReturn(List.of(grant));
        when(teamMapper.selectIdsByDivisionId(118L)).thenReturn(List.of(1001L, 1002L));

        closingService.finishDivision(118L, "赛事正常结束");

        verify(captainGrantMapper).expireActiveByDivisionId(eq(118L), any(Date.class));
        verify(grantLogSupport, times(1)).writeGrantLog(any(), any(), any(), any(), anyString(), anyString(), anyString());
        verify(teamMapper).archiveActiveByDivisionId(eq(118L),
                eq(TeamStatusEnum.ACTIVE.getCode()), eq(TeamStatusEnum.ARCHIVED.getCode()), any(Date.class));
        verify(teamMemberMapper).historicalizeActiveByDivisionId(eq(118L), eq(1), eq(0), any(Date.class), eq("DIVISION_FINISHED"));
        verify(lifecycleClosingMapper).expirePendingInvitationsByTeamIds(eq(List.of(1001L, 1002L)), eq(0), eq(3), any(Date.class));
        verify(lifecycleClosingMapper).cancelPendingApplicationsByTeamIds(eq(List.of(1001L, 1002L)), eq(0), eq(3), any(Date.class));
        verify(playerProfileMapper).clearCurrentTeamIdByTeamIds(eq(List.of(1001L, 1002L)));
        verify(seasonService).refreshAggregatedStatus(3L);
    }

    @Test
    void finish_casZeroButAlreadyFinished_isIdempotent() {
        when(divisionMapper.selectById(118L))
                .thenReturn(division(118L, DivisionStatusEnum.PLAYING.getCode()))
                .thenReturn(division(118L, DivisionStatusEnum.FINISHED.getCode()));
        when(lifecycleClosingMapper.casFinishDivision(eq(118L), anyCollection(), any(Date.class))).thenReturn(0);

        assertDoesNotThrow(() -> closingService.finishDivision(118L, null));
        verify(captainGrantMapper, never()).expireActiveByDivisionId(anyLong(), any());
        verify(seasonService).refreshAggregatedStatus(3L);
    }

    @Test
    void finish_memberUpdateThrows_bubblesUpForRollback() {
        when(divisionMapper.selectById(118L))
                .thenReturn(division(118L, DivisionStatusEnum.SCHEDULED.getCode()));
        when(lifecycleClosingMapper.casFinishDivision(eq(118L), anyCollection(), any(Date.class))).thenReturn(1);
        when(captainGrantMapper.selectActiveByDivisionId(118L)).thenReturn(Collections.emptyList());
        when(teamMapper.selectIdsByDivisionId(118L)).thenReturn(List.of(1L));
        when(teamMemberMapper.historicalizeActiveByDivisionId(anyLong(), anyInt(), anyInt(), any(), anyString()))
                .thenThrow(new RuntimeException("db fail"));

        assertThrows(RuntimeException.class, () -> closingService.finishDivision(118L, null));
    }

    private BasketDivision division(Long id, String status) {
        BasketDivision d = new BasketDivision();
        d.setId(id);
        d.setSeasonId(3L);
        d.setDivisionStatus(status);
        d.setIsDelete(0);
        return d;
    }
}
