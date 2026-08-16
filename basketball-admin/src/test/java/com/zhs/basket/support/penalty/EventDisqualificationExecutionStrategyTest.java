package com.zhs.basket.support.penalty;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.dto.v4.BasketMatchForfeitReq;
import com.zhs.basket.dto.v4.BasketSeriesForfeitReq;
import com.zhs.basket.enums.DisciplinePenaltyCodeEnum;
import com.zhs.basket.enums.PenaltyExecutionStatusEnum;
import com.zhs.basket.enums.v4.EventTeamStatusEnum;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.service.v4.IBasketV4StatsSettlementService;
import com.zhs.basket.service.v4.IStageExecutionExceptionApplicationService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.v4.schedule.WinnerOfSeriesAdvanceSupport;
import com.zhs.basket.vo.PenaltyExecutionResultVo;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventDisqualificationExecutionStrategyTest {

    @Mock private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Mock private com.zhs.basket.mapper.BasketDisciplineExecutionDetailMapper executionDetailMapper;
    @Mock private BasketEventTeamMapper eventTeamMapper;
    @Mock private BasketV4MatchMapper matchMapper;
    @Mock private BasketMatchSeriesMapper seriesMapper;
    @Mock private BasketDivisionStageMapper stageMapper;
    @Mock private BasketDivisionStageQualificationMapper qualificationMapper;
    @Mock private IStageExecutionExceptionApplicationService exceptionService;
    @Mock private IBasketV4StatsSettlementService statsSettlementService;
    @Mock private BasketBusinessAuditWriter auditWriter;
    @Mock private WinnerOfSeriesAdvanceSupport winnerOfSeriesAdvanceSupport;

    @InjectMocks
    private EventDisqualificationExecutionStrategy strategy;

    private BasketDisciplineRecord record;
    private BasketEventTeam eventTeam;

    @BeforeEach
    void setUp() {
        record = new BasketDisciplineRecord();
        record.setId(9L);
        record.setPenaltyCode(DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION.name());
        record.setEventTeamId(101L);
        eventTeam = new BasketEventTeam();
        eventTeam.setId(101L);
        eventTeam.setDivisionId(10L);
        eventTeam.setSeasonId(1L);
        eventTeam.setTeamNameSnapshot("金鲨");
        eventTeam.setEventTeamStatus(EventTeamStatusEnum.ACTIVE.getCode());
    }

    @Test
    void supportsOnlyEventDisqualification() {
        assertTrue(strategy.supports(record));
        record.setPenaltyCode(DisciplinePenaltyCodeEnum.WARNING.name());
        assertTrue(!strategy.supports(record));
    }

    @Test
    void execute_forfeitsFutureStandaloneAndKeepsFinished() {
        when(eventTeamMapper.selectById(101L)).thenReturn(eventTeam);
        BasketV4Match finished = match(1L, null, MatchStatusEnum.FINISHED.getCode(), "PUBLISHED");
        finished.setHomeScore(88);
        finished.setAwayScore(70);
        BasketV4Match future = match(2L, null, MatchStatusEnum.SCHEDULED.getCode(), "PUBLISHED");
        BasketV4Match seriesChild = match(3L, 20L, MatchStatusEnum.SCHEDULED.getCode(), "PUBLISHED");
        when(matchMapper.selectByDivisionAndEventTeam(10L, 101L)).thenReturn(List.of(finished, future, seriesChild));
        when(seriesMapper.selectByDivisionId(10L)).thenReturn(List.of());
        when(stageMapper.selectByDivisionId(10L)).thenReturn(List.of());

        PenaltyExecutionResultVo result = strategy.execute(record);

        assertEquals(PenaltyExecutionStatusEnum.EFFECTIVE.name(), result.getExecutionStatus());
        assertEquals(2, result.getFutureMatchForfeitCount());
        ArgumentCaptor<BasketMatchForfeitReq> captor = ArgumentCaptor.forClass(BasketMatchForfeitReq.class);
        verify(exceptionService, times(1)).forfeitMatch(captor.capture());
        assertEquals(2L, captor.getValue().getMatchId());
        assertEquals(101L, captor.getValue().getLoserEventTeamId());
        ArgumentCaptor<BasketMatchForfeitReq> overrideCaptor = ArgumentCaptor.forClass(BasketMatchForfeitReq.class);
        verify(exceptionService).overrideMatchOfficialResultForDisqualification(overrideCaptor.capture());
        assertEquals(1L, overrideCaptor.getValue().getMatchId());
        verify(eventTeamMapper).updateById(any());
        verify(statsSettlementService).rebuildDivisionStats(10L);
    }

    @Test
    void execute_forfeitsOpenSeriesAndGuardsDownstream() {
        when(eventTeamMapper.selectById(101L)).thenReturn(eventTeam);
        when(matchMapper.selectByDivisionAndEventTeam(10L, 101L)).thenReturn(List.of());
        BasketMatchSeries open = new BasketMatchSeries();
        open.setId(20L);
        open.setHomeEventTeamId(101L);
        open.setAwayEventTeamId(202L);
        open.setSeriesStatus(MatchSeriesStatusEnum.READY.getCode());
        BasketMatchSeries blocked = new BasketMatchSeries();
        blocked.setId(21L);
        blocked.setHomeEventTeamId(101L);
        blocked.setAwayEventTeamId(203L);
        blocked.setSeriesStatus(MatchSeriesStatusEnum.RUNNING.getCode());
        when(seriesMapper.selectByDivisionId(10L)).thenReturn(List.of(open, blocked));
        when(stageMapper.selectByDivisionId(10L)).thenReturn(List.of());
        org.mockito.Mockito.doAnswer(invocation -> {
            BasketSeriesForfeitReq req = invocation.getArgument(0);
            if (req != null && Long.valueOf(21L).equals(req.getSeriesId())) {
                throw new ServiceException(WinnerOfSeriesAdvanceSupport.MSG_DOWNSTREAM_STARTED);
            }
            return null;
        }).when(exceptionService).forfeitSeries(any(BasketSeriesForfeitReq.class));

        PenaltyExecutionResultVo result = strategy.execute(record);

        assertEquals(PenaltyExecutionStatusEnum.PARTIAL.name(), result.getExecutionStatus());
        assertEquals(1, result.getSeriesForfeitCount());
        assertEquals(1, result.getQualificationBlockedCount());
        verify(exceptionService, times(2)).forfeitSeries(any(BasketSeriesForfeitReq.class));
        verify(exceptionService, never()).forfeitMatch(any());
    }

    @Test
    void execute_warnsWhenCompletedSeriesDownstreamStarted() {
        when(eventTeamMapper.selectById(101L)).thenReturn(eventTeam);
        when(matchMapper.selectByDivisionAndEventTeam(10L, 101L)).thenReturn(List.of());
        BasketMatchSeries done = new BasketMatchSeries();
        done.setId(30L);
        done.setHomeEventTeamId(101L);
        done.setAwayEventTeamId(202L);
        done.setWinnerEventTeamId(101L);
        done.setSeriesStatus(MatchSeriesStatusEnum.COMPLETED.getCode());
        when(seriesMapper.selectByDivisionId(10L)).thenReturn(List.of(done));
        BasketMatchSeries next = new BasketMatchSeries();
        next.setId(31L);
        when(seriesMapper.selectBySourceSeriesId(30L)).thenReturn(List.of(next));
        when(winnerOfSeriesAdvanceSupport.isDownstreamStarted(next)).thenReturn(true);
        when(stageMapper.selectByDivisionId(10L)).thenReturn(List.of());

        PenaltyExecutionResultVo result = strategy.execute(record);

        assertEquals(PenaltyExecutionStatusEnum.PARTIAL.name(), result.getExecutionStatus());
        assertEquals(1, result.getQualificationBlockedCount());
        verify(exceptionService, never()).forfeitSeries(any());
        verify(winnerOfSeriesAdvanceSupport, never()).rewireAfterSeriesResult(any());
    }

    @Test
    void execute_clearsUnstartedDownstreamWithoutRewritingHistory() {
        when(eventTeamMapper.selectById(101L)).thenReturn(eventTeam);
        when(matchMapper.selectByDivisionAndEventTeam(10L, 101L)).thenReturn(List.of());
        BasketMatchSeries done = new BasketMatchSeries();
        done.setId(30L);
        done.setHomeEventTeamId(101L);
        done.setAwayEventTeamId(202L);
        done.setWinnerEventTeamId(101L);
        done.setSeriesStatus(MatchSeriesStatusEnum.COMPLETED.getCode());
        when(seriesMapper.selectByDivisionId(10L)).thenReturn(List.of(done));
        BasketMatchSeries next = new BasketMatchSeries();
        next.setId(31L);
        next.setHomeEventTeamId(101L);
        next.setSeriesStatus(MatchSeriesStatusEnum.READY.getCode());
        when(seriesMapper.selectBySourceSeriesId(30L)).thenReturn(List.of(next));
        when(winnerOfSeriesAdvanceSupport.isDownstreamStarted(next)).thenReturn(false);
        when(stageMapper.selectByDivisionId(10L)).thenReturn(List.of());

        PenaltyExecutionResultVo result = strategy.execute(record);

        assertEquals(PenaltyExecutionStatusEnum.PARTIAL.name(), result.getExecutionStatus());
        verify(exceptionService).applySeriesOfficialSweepForDisqualification(any());
        verify(winnerOfSeriesAdvanceSupport, never()).rewireAfterSeriesResult(any());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("\u7ba1\u7406\u5458")));
    }

    @Test
    void execute_idempotentWhenAlreadyDisqualified() {
        eventTeam.setEventTeamStatus(EventTeamStatusEnum.DISQUALIFIED.getCode());
        when(eventTeamMapper.selectById(101L)).thenReturn(eventTeam);
        when(matchMapper.selectByDivisionAndEventTeam(10L, 101L)).thenReturn(List.of());
        when(seriesMapper.selectByDivisionId(10L)).thenReturn(List.of());
        when(stageMapper.selectByDivisionId(10L)).thenReturn(List.of());

        PenaltyExecutionResultVo result = strategy.execute(record);

        assertEquals(PenaltyExecutionStatusEnum.EFFECTIVE.name(), result.getExecutionStatus());
        verify(eventTeamMapper, never()).updateById(any());
    }

    private BasketV4Match match(Long id, Long seriesId, String status, String publish) {
        BasketV4Match match = new BasketV4Match();
        match.setId(id);
        match.setSeriesId(seriesId);
        match.setMatchStatus(status);
        match.setPublishStatus(publish);
        match.setHomeEventTeamId(101L);
        match.setAwayEventTeamId(202L);
        return match;
    }
}
