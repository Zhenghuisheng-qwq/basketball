package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketPointAdjustment;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageQualification;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketStanding;
import com.zhs.basket.dto.v4.BasketStandingPointAdjustmentReq;
import com.zhs.basket.enums.v4.StageExecutionStatusEnum;
import com.zhs.basket.mapper.BasketPointAdjustmentMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketStandingMapper;
import com.zhs.basket.service.v4.IBasketV4StatsSettlementService;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import org.springframework.context.ApplicationEventPublisher;
import com.zhs.basket.support.v4.MatchSeriesQualificationSyncService;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StandingPointAdjustmentApplicationServiceImplTest {

    @Mock private BasketDivisionMapper divisionMapper;
    @Mock private BasketDivisionStageMapper stageMapper;
    @Mock private BasketEventTeamMapper eventTeamMapper;
    @Mock private BasketStandingMapper standingMapper;
    @Mock private BasketPointAdjustmentMapper pointAdjustmentMapper;
    @Mock private BasketDivisionStageQualificationMapper qualificationMapper;
    @Mock private MatchSeriesQualificationSyncService qualificationSyncService;
    @Mock private IBasketV4StatsSettlementService statsSettlementService;
    @Mock private IDivisionStageExecutionApplicationService stageExecutionApplicationService;
    @Mock private BasketBusinessAuditWriter auditWriter;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private StandingPointAdjustmentApplicationServiceImpl service;

    private BasketDivisionStage leagueStage;
    private BasketEventTeam eventTeam;

    @BeforeEach
    void setUp() {
        leagueStage = new BasketDivisionStage();
        leagueStage.setId(20L);
        leagueStage.setDivisionId(10L);
        leagueStage.setCompetitionFormat("ROUND_ROBIN");
        leagueStage.setExecutionStatus(StageExecutionStatusEnum.RUNNING.getCode());

        eventTeam = new BasketEventTeam();
        eventTeam.setId(100L);
        eventTeam.setDivisionId(10L);
        eventTeam.setSeasonId(1L);
        eventTeam.setTeamNameSnapshot("TITANS");
    }

    @Test
    void caseA_leagueAdjustMinusTwo() {
        stubHappyPath();
        BasketStanding standing = new BasketStanding();
        standing.setEventTeamId(100L);
        standing.setStandingPoints(12);
        when(standingMapper.selectByStageId(20L)).thenReturn(List.of(standing));
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of());

        apply(req(-2, "违规处罚", "spa-a"));

        ArgumentCaptor<BasketPointAdjustment> captor = ArgumentCaptor.forClass(BasketPointAdjustment.class);
        verify(pointAdjustmentMapper).insert(captor.capture());
        assertEquals(-2, captor.getValue().getAdjustmentPoints());
        assertEquals(100L, captor.getValue().getEventTeamId());
        assertEquals(10L, captor.getValue().getDivisionId());
        assertEquals(20L, captor.getValue().getStageId());
        verify(statsSettlementService).rebuildStageStats(20L);
        verify(auditWriter).writeAudit(eq("V4_STANDING"), eq("EVENT_TEAM"), eq(100L),
                eq("POINT_ADJUSTMENT"), any(), eq("违规处罚"), any());
    }

    @Test
    void caseB_secondAdjustmentPlusOne() {
        stubHappyPath();
        BasketStanding standing = new BasketStanding();
        standing.setEventTeamId(100L);
        standing.setStandingPoints(10);
        when(standingMapper.selectByStageId(20L)).thenReturn(List.of(standing));
        BasketPointAdjustment existingSum = new BasketPointAdjustment();
        existingSum.setEventTeamId(100L);
        existingSum.setAdjustmentPoints(-2);
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of(existingSum));

        apply(req(1, "赛事仲裁", "spa-b"));

        ArgumentCaptor<BasketPointAdjustment> captor = ArgumentCaptor.forClass(BasketPointAdjustment.class);
        verify(pointAdjustmentMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getAdjustmentPoints());
        verify(statsSettlementService).rebuildStageStats(20L);
    }

    @Test
    void caseG_personalTempEventTeamAllowed() {
        eventTeam.setSourceTeamId(null);
        eventTeam.setSourceDrawTeamId(88L);
        stubHappyPath();
        when(standingMapper.selectByStageId(20L)).thenReturn(List.of());
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of());

        apply(req(-1, "数据纠错", "spa-g"));

        verify(pointAdjustmentMapper).insert(any());
        verify(statsSettlementService).rebuildStageStats(20L);
    }

    @Test
    void caseH_knockoutRejected() {
        leagueStage.setCompetitionFormat("SINGLE_ELIMINATION");
        when(divisionMapper.selectByIdForUpdate(10L)).thenReturn(division());
        when(stageMapper.selectById(20L)).thenReturn(leagueStage);

        ServiceException ex = assertThrows(ServiceException.class, () -> apply(req(-2, "违规处罚", "spa-h")));
        assertEquals(StandingPointAdjustmentApplicationServiceImpl.MSG_KNOCKOUT, ex.getMessage());
        verify(pointAdjustmentMapper, never()).insert(any());
    }

    @Test
    void caseI_qualificationNotConfirmedAllowed() {
        stubHappyPath();
        when(qualificationMapper.selectConfirmedByStageId(20L)).thenReturn(List.of());
        when(standingMapper.selectByStageId(20L)).thenReturn(List.of());
        when(pointAdjustmentMapper.selectV4SumByStage(10L, 20L)).thenReturn(List.of());

        apply(req(-2, "违规处罚", "spa-i"));

        verify(pointAdjustmentMapper).insert(any());
    }

    @Test
    void caseJ_qualificationConfirmedRejected() {
        when(divisionMapper.selectByIdForUpdate(10L)).thenReturn(division());
        when(stageMapper.selectById(20L)).thenReturn(leagueStage);
        when(qualificationMapper.selectConfirmedByStageId(20L))
                .thenReturn(List.of(new BasketDivisionStageQualification()));

        ServiceException ex = assertThrows(ServiceException.class, () -> apply(req(-2, "违规处罚", "spa-j")));
        assertEquals(StandingPointAdjustmentApplicationServiceImpl.MSG_CONFIRMED, ex.getMessage());
        verify(pointAdjustmentMapper, never()).insert(any());
    }

    @Test
    void caseK_downstreamStartedRejected() {
        when(divisionMapper.selectByIdForUpdate(10L)).thenReturn(division());
        when(stageMapper.selectById(20L)).thenReturn(leagueStage);
        when(qualificationMapper.selectConfirmedByStageId(20L)).thenReturn(List.of());
        when(qualificationSyncService.hasOfficialDownstreamStarted(leagueStage)).thenReturn(true);

        ServiceException ex = assertThrows(ServiceException.class, () -> apply(req(-2, "违规处罚", "spa-k")));
        assertEquals(StandingPointAdjustmentApplicationServiceImpl.MSG_DOWNSTREAM, ex.getMessage());
        verify(pointAdjustmentMapper, never()).insert(any());
    }

    @Test
    void idempotentClientRequestDoesNotInsertTwice() {
        stubHappyPath();
        BasketPointAdjustment existing = new BasketPointAdjustment();
        existing.setId(9L);
        existing.setEventTeamId(100L);
        existing.setAdjustmentPoints(-2);
        existing.setAdjustmentReason("违规处罚");
        when(pointAdjustmentMapper.selectByIdempotencyKey("spa-dup")).thenReturn(existing);

        apply(req(-2, "违规处罚", "spa-dup"));

        verify(pointAdjustmentMapper, never()).insert(any());
        verify(statsSettlementService, never()).rebuildStageStats(anyLong());
    }

    @Test
    void deltaZeroRejected() {
        ServiceException ex = assertThrows(ServiceException.class, () -> apply(req(0, "违规处罚", "spa-0")));
        assertTrue(ex.getMessage().contains("0"));
    }

    private void stubHappyPath() {
        when(divisionMapper.selectByIdForUpdate(10L)).thenReturn(division());
        when(stageMapper.selectById(20L)).thenReturn(leagueStage);
        when(eventTeamMapper.selectById(100L)).thenReturn(eventTeam);
        when(qualificationMapper.selectConfirmedByStageId(20L)).thenReturn(List.of());
        when(qualificationSyncService.hasOfficialDownstreamStarted(leagueStage)).thenReturn(false);
        when(pointAdjustmentMapper.selectByIdempotencyKey(any())).thenReturn(null);
    }

    private void apply(BasketStandingPointAdjustmentReq req) {
        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getUserId).thenReturn(1L);
            mocked.when(SecurityUtils::getUsername).thenReturn("admin");
            service.applyAdjustment(req);
        }
    }

    private BasketStandingPointAdjustmentReq req(int delta, String reason, String clientRequestId) {
        BasketStandingPointAdjustmentReq req = new BasketStandingPointAdjustmentReq();
        req.setDivisionId(10L);
        req.setStageId(20L);
        req.setEventTeamId(100L);
        req.setDelta(delta);
        req.setReason(reason);
        req.setClientRequestId(clientRequestId);
        return req;
    }

    private BasketDivision division() {
        BasketDivision division = new BasketDivision();
        division.setId(10L);
        division.setSeasonId(1L);
        return division;
    }
}
