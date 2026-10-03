package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketTeamLifecycleClosingMapper;
import com.zhs.basket.service.v4.IBasketV4SeasonService;
import com.zhs.basket.support.v4.DivisionSafeRecoverySupport;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketV4DivisionRecoveryServiceTest {

    @InjectMocks
    private BasketV4DivisionServiceImpl divisionService;
    @InjectMocks
    private BasketV4DivisionClosingServiceImpl closingService;

    @Mock
    private BasketDivisionMapper divisionMapper;
    @Mock
    private DivisionSafeRecoverySupport safeRecoverySupport;
    @Mock
    private IBasketV4SeasonService seasonService;
    @Mock
    private com.zhs.basket.support.v4.V4PublicReadCachePublisher publicReadCachePublisher;
    @Mock
    private BasketTeamLifecycleClosingMapper lifecycleClosingMapper;
    @Mock
    private com.zhs.basket.mapper.v4.BasketCaptainGrantMapper captainGrantMapper;
    @Mock
    private com.zhs.basket.mapper.BasketTeamMapper teamMapper;
    @Mock
    private com.zhs.basket.mapper.BasketTeamMemberMapper teamMemberMapper;
    @Mock
    private com.zhs.basket.mapper.BasketPlayerProfileMapper playerProfileMapper;
    @Mock
    private com.zhs.basket.support.CaptainGrantLogSupport grantLogSupport;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Test
    void deleteCleansFormatConfigAndRefreshesSeason() {
        BasketDivision division = new BasketDivision();
        division.setId(40L);
        division.setSeasonId(7L);
        division.setDivisionStatus(DivisionStatusEnum.FINISHED.getCode());
        division.setIsDelete(0);
        when(divisionMapper.selectById(40L)).thenReturn(division);
        when(divisionMapper.softDelete(40L)).thenReturn(1);

        divisionService.delete(40L);

        verify(safeRecoverySupport).validateDivisionCanDelete(division);
        verify(safeRecoverySupport).softDeleteFormatConfig(40L);
        verify(divisionMapper).softDelete(40L);
        verify(seasonService).refreshAggregatedStatus(7L);
        verify(publicReadCachePublisher).divisionMutation(40L, 7L);
    }

    @Test
    void case13_closeRegistrationWithoutEvidenceCannotFinish() {
        BasketDivision division = finishable(50L);
        when(divisionMapper.selectById(50L)).thenReturn(division);
        doThrow(new ServiceException(DivisionSafeRecoverySupport.FINISH_BLOCKED_MESSAGE))
                .when(safeRecoverySupport).assertCanFinish(50L);

        ServiceException ex = assertThrows(ServiceException.class, () -> closingService.finishDivision(50L, "误结束"));

        assertEquals(DivisionSafeRecoverySupport.FINISH_BLOCKED_MESSAGE, ex.getMessage());
        verify(lifecycleClosingMapper, never()).casFinishDivision(eq(50L), any(), any());
    }

    @Test
    void case14_officialEvidenceKeepsFinish() {
        BasketDivision division = finishable(51L);
        when(divisionMapper.selectById(51L)).thenReturn(division);
        when(lifecycleClosingMapper.casFinishDivision(eq(51L), any(), any())).thenReturn(1);
        when(captainGrantMapper.selectActiveByDivisionId(51L)).thenReturn(Collections.emptyList());
        when(teamMapper.selectIdsByDivisionId(51L)).thenReturn(Collections.emptyList());

        closingService.finishDivision(51L, "赛事正常结束");

        verify(safeRecoverySupport).assertCanFinish(51L);
        verify(lifecycleClosingMapper).casFinishDivision(eq(51L), any(), any());
        verify(seasonService).refreshAggregatedStatus(8L);
    }

    private BasketDivision finishable(Long id) {
        BasketDivision division = new BasketDivision();
        division.setId(id);
        division.setSeasonId(8L);
        division.setIsDelete(0);
        division.setDivisionStatus(DivisionStatusEnum.REGISTRATION_CLOSED.getCode());
        return division;
    }
}
