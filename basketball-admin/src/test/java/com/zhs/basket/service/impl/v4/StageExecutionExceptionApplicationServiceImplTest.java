package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageQualification;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.dto.v4.BasketManualAdvanceReq;
import com.zhs.basket.dto.v4.BasketMatchForfeitReq;
import com.zhs.basket.dto.v4.BasketPairingAdjustReq;
import com.zhs.basket.dto.v4.BasketPairingSlotReq;
import com.zhs.basket.dto.v4.BasketSeriesForfeitReq;
import com.zhs.basket.dto.v4.BasketSeriesRevokeResolutionReq;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.QualificationTypeEnum;
import com.zhs.basket.enums.v4.SeriesResolutionTypeEnum;
import com.zhs.basket.enums.v4.StageExecutionStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.service.v4.IBasketMatchSeriesService;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.v4.MatchSeriesQualificationSyncService;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StageExecutionExceptionApplicationServiceImplTest {

    @Mock private BasketV4MatchMapper matchMapper;
    @Mock private BasketMatchSeriesMapper seriesMapper;
    @Mock private BasketDivisionStageMapper stageMapper;
    @Mock private BasketDivisionStageTeamMapper stageTeamMapper;
    @Mock private BasketDivisionStageQualificationMapper qualificationMapper;
    @Mock private BasketScheduleBatchMapper scheduleBatchMapper;
    @Mock private MatchSeriesQualificationSyncService qualificationSyncService;
    @Mock private IBasketMatchSeriesService matchSeriesService;
    @Mock private IDivisionStageExecutionApplicationService stageExecutionService;
    @Mock private BasketBusinessAuditWriter auditWriter;

    @InjectMocks
    private StageExecutionExceptionApplicationServiceImpl service;

    private BasketDivisionStage eliminationStage;
    private BasketV4Match match;

    @BeforeEach
    void setUp() {
        eliminationStage = new BasketDivisionStage();
        eliminationStage.setId(10L);
        eliminationStage.setDivisionId(100L);
        eliminationStage.setStageCode("QF");
        eliminationStage.setStageName("八强");
        eliminationStage.setCompetitionFormat("SINGLE_ELIMINATION");
        eliminationStage.setAdvancementType("WINNER_ADVANCE");
        eliminationStage.setNextStageCode("SF");
        eliminationStage.setBestOf(1);
        eliminationStage.setScheduleBatchId(500L);
        eliminationStage.setExecutionStatus(StageExecutionStatusEnum.SCHEDULE_GENERATED.getCode());

        match = new BasketV4Match();
        match.setId(1L);
        match.setStageId(10L);
        match.setHomeEventTeamId(101L);
        match.setAwayEventTeamId(102L);
        match.setMatchStatus(MatchStatusEnum.SCHEDULED.getCode());
    }

    @Test
    void caseA_matchForfeit_setsWinnerAndRebuildsSeries() {
        match.setSeriesId(20L);
        when(matchMapper.selectById(1L)).thenReturn(match);
        when(matchMapper.updateById(any())).thenReturn(1);

        BasketMatchForfeitReq req = new BasketMatchForfeitReq();
        req.setMatchId(1L);
        req.setLoserEventTeamId(102L);
        req.setReason("对手未到");

        service.forfeitMatch(req);

        verify(matchMapper).updateById(any());
        verify(matchSeriesService).rebuildSeriesResult(20L);
        verify(auditWriter).writeAudit(any(), any(), anyLong(), any(), any(), any(), any());
    }

    @Test
    void caseB_seriesForfeit_completesWithResolution() {
        BasketMatchSeries series = new BasketMatchSeries();
        series.setId(20L);
        series.setStageId(10L);
        series.setHomeEventTeamId(101L);
        series.setAwayEventTeamId(102L);
        series.setHomeWinCount(1);
        series.setAwayWinCount(1);
        series.setSeriesStatus(MatchSeriesStatusEnum.RUNNING.getCode());
        when(seriesMapper.selectById(20L)).thenReturn(series);
        when(stageMapper.selectById(10L)).thenReturn(eliminationStage);
        when(seriesMapper.updateResolution(any())).thenReturn(1);
        doNothing().when(qualificationSyncService).assertCanModifyDownstream(any());
        doNothing().when(qualificationSyncService).syncAfterSeriesRebuild(any(), any(), any(), any());

        BasketSeriesForfeitReq req = new BasketSeriesForfeitReq();
        req.setSeriesId(20L);
        req.setForfeitEventTeamId(102L);
        req.setReason("全队退赛");

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getUserId).thenReturn(1L);
            service.forfeitSeries(req);
        }

        verify(seriesMapper).updateResolution(any());
        verify(stageExecutionService).checkStageCompletion(10L);
    }

    @Test
    void caseC_manualAdvance_insertsPreviewQualification() {
        when(stageMapper.selectById(10L)).thenReturn(eliminationStage);
        when(stageMapper.selectByDivisionId(100L)).thenReturn(List.of(eliminationStage, nextStage()));
        when(stageTeamMapper.existsByStageAndEventTeam(10L, 101L)).thenReturn(true);
        when(qualificationMapper.selectActiveByStageAndTeamAndType(10L, 101L, QualificationTypeEnum.MANUAL.getCode()))
                .thenReturn(null);
        when(qualificationMapper.insert(any())).thenReturn(1);
        doNothing().when(qualificationSyncService).assertCanModifyDownstream(any());

        BasketManualAdvanceReq req = new BasketManualAdvanceReq();
        req.setStageId(10L);
        req.setEventTeamId(101L);
        req.setReason("赛务裁定");

        service.manualAdvance(req);

        verify(qualificationMapper).insert(any());
    }

    @Test
    void caseD_pairingAdjustBo1_regeneratesMatches() {
        when(stageMapper.selectById(10L)).thenReturn(eliminationStage);
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setId(500L);
        batch.setSeasonId(1L);
        batch.setPublishStatus(PublishStatusEnum.DRAFT.getCode());
        when(scheduleBatchMapper.selectById(500L)).thenReturn(batch);
        when(stageTeamMapper.selectByStageId(10L)).thenReturn(List.of(
                stageTeam(101L), stageTeam(102L), stageTeam(103L), stageTeam(104L)));
        when(matchMapper.insert(any())).thenReturn(1);

        BasketPairingAdjustReq req = new BasketPairingAdjustReq();
        req.setStageId(10L);
        req.setReason("运营调整");
        req.setSlots(List.of(
                slot(0, 101L, 103L),
                slot(1, 102L, 104L)));

        service.adjustPairing(req);

        verify(matchMapper).softDeleteDraftByBatchId(500L);
        verify(matchMapper, times(2)).insert(any());
    }

    @Test
    void caseE_pairingAdjustBo3_regeneratesSeriesAndGames() {
        eliminationStage.setBestOf(3);
        when(stageMapper.selectById(10L)).thenReturn(eliminationStage);
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setId(500L);
        batch.setSeasonId(1L);
        batch.setPublishStatus(PublishStatusEnum.DRAFT.getCode());
        when(scheduleBatchMapper.selectById(500L)).thenReturn(batch);
        when(stageTeamMapper.selectByStageId(10L)).thenReturn(List.of(stageTeam(101L), stageTeam(102L)));
        when(seriesMapper.insert(any())).thenAnswer(inv -> {
            BasketMatchSeries s = inv.getArgument(0);
            s.setId(99L);
            return 1;
        });
        when(matchMapper.insert(any())).thenReturn(1);

        BasketPairingAdjustReq req = new BasketPairingAdjustReq();
        req.setStageId(10L);
        req.setReason("调整");
        req.setSlots(List.of(slot(0, 101L, 102L)));

        service.adjustPairing(req);

        verify(seriesMapper).insert(any());
        verify(matchMapper, times(3)).insert(any());
    }

    @Test
    void caseF_downstreamProtection_blocksManualAdvance() {
        when(stageMapper.selectById(10L)).thenReturn(eliminationStage);
        doThrow(new ServiceException("下一阶段已生成或发布赛程"))
                .when(qualificationSyncService).assertCanModifyDownstream(any());

        BasketManualAdvanceReq req = new BasketManualAdvanceReq();
        req.setStageId(10L);
        req.setEventTeamId(101L);
        req.setReason("test");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.manualAdvance(req));
        assertTrue(ex.getMessage().contains("下一阶段"));
    }

    @Test
    void caseG_duplicateManualAdvance_isIdempotent() {
        when(stageMapper.selectById(10L)).thenReturn(eliminationStage);
        when(stageTeamMapper.existsByStageAndEventTeam(10L, 101L)).thenReturn(true);
        when(qualificationMapper.selectActiveByStageAndTeamAndType(10L, 101L, QualificationTypeEnum.MANUAL.getCode()))
                .thenReturn(new BasketDivisionStageQualification());
        doNothing().when(qualificationSyncService).assertCanModifyDownstream(any());

        BasketManualAdvanceReq req = new BasketManualAdvanceReq();
        req.setStageId(10L);
        req.setEventTeamId(101L);
        req.setReason("重复");

        assertDoesNotThrow(() -> service.manualAdvance(req));
        verify(qualificationMapper, never()).insert(any());
    }

    @Test
    void caseH_duplicateSeriesForfeit_isIdempotent() {
        BasketMatchSeries series = new BasketMatchSeries();
        series.setId(20L);
        series.setStageId(10L);
        series.setHomeEventTeamId(101L);
        series.setAwayEventTeamId(102L);
        series.setResolutionType(SeriesResolutionTypeEnum.FORFEIT.getCode());
        when(seriesMapper.selectById(20L)).thenReturn(series);

        BasketSeriesForfeitReq req = new BasketSeriesForfeitReq();
        req.setSeriesId(20L);
        req.setForfeitEventTeamId(102L);
        req.setReason("重复");

        assertDoesNotThrow(() -> service.forfeitSeries(req));
        verify(seriesMapper, never()).updateResolution(any());
    }

    @Test
    void caseI_publishedScheduleAdjust_rejected() {
        when(stageMapper.selectById(10L)).thenReturn(eliminationStage);
        BasketScheduleBatch batch = new BasketScheduleBatch();
        batch.setId(500L);
        batch.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        when(scheduleBatchMapper.selectById(500L)).thenReturn(batch);

        BasketPairingAdjustReq req = new BasketPairingAdjustReq();
        req.setStageId(10L);
        req.setReason("test");
        req.setSlots(List.of(slot(0, 101L, 102L)));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.adjustPairing(req));
        assertTrue(ex.getMessage().contains("草稿"));
    }

    @Test
    void caseJ_invalidLoser_rejected() {
        when(matchMapper.selectById(1L)).thenReturn(match);

        BasketMatchForfeitReq req = new BasketMatchForfeitReq();
        req.setMatchId(1L);
        req.setLoserEventTeamId(999L);
        req.setReason("test");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.forfeitMatch(req));
        assertTrue(ex.getMessage().contains("双方"));
    }

    @Test
    void revokeSeriesResolution_clearsAndRebuilds() {
        BasketMatchSeries series = new BasketMatchSeries();
        series.setId(20L);
        series.setStageId(10L);
        series.setResolutionType(SeriesResolutionTypeEnum.FORFEIT.getCode());
        when(seriesMapper.selectById(20L)).thenReturn(series);
        when(stageMapper.selectById(10L)).thenReturn(eliminationStage);
        when(seriesMapper.clearResolution(20L)).thenReturn(1);
        doNothing().when(qualificationSyncService).assertCanModifyDownstream(any());

        BasketSeriesRevokeResolutionReq req = new BasketSeriesRevokeResolutionReq();
        req.setSeriesId(20L);
        req.setReason("误判撤销");

        service.revokeSeriesResolution(req);

        verify(seriesMapper).clearResolution(20L);
        verify(matchSeriesService).rebuildSeriesResult(20L);
    }

    private static BasketDivisionStage nextStage() {
        BasketDivisionStage next = new BasketDivisionStage();
        next.setId(11L);
        next.setStageCode("SF");
        next.setExecutionStatus(StageExecutionStatusEnum.READY.getCode());
        return next;
    }

    private static BasketDivisionStageTeam stageTeam(Long eventTeamId) {
        BasketDivisionStageTeam t = new BasketDivisionStageTeam();
        t.setEventTeamId(eventTeamId);
        return t;
    }

    private static BasketPairingSlotReq slot(int bracketSlot, long home, long away) {
        BasketPairingSlotReq s = new BasketPairingSlotReq();
        s.setBracketSlot(bracketSlot);
        s.setHomeEventTeamId(home);
        s.setAwayEventTeamId(away);
        return s;
    }
}
