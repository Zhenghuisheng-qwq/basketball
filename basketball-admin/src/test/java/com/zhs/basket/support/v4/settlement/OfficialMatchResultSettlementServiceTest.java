package com.zhs.basket.support.v4.settlement;

import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.OfficialResultSource;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchTeamStatMapper;
import com.zhs.basket.service.v4.IBasketMatchSeriesService;
import com.zhs.basket.service.v4.IBasketV4StatsSettlementService;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zhs.basket.domain.v4.BasketV4MatchTeamStat;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficialMatchResultSettlementServiceTest {

    @Mock private BasketV4MatchMapper matchMapper;
    @Mock private BasketV4MatchTeamStatMapper matchTeamStatMapper;
    @Mock private BasketV4MatchPlayerStatMapper matchPlayerStatMapper;
    @Mock private IBasketMatchSeriesService matchSeriesService;
    @Mock private IBasketV4StatsSettlementService statsSettlementService;
    @InjectMocks private OfficialMatchResultSettlementService service;

    @Test
    void settle_adminDirect_writesMatchAndRebuilds() {
        BasketV4Match match = scheduled();
        when(matchMapper.selectByIdForUpdate(1L)).thenReturn(match);
        when(matchMapper.updateById(any())).thenReturn(1);

        OfficialMatchResultCommand command = scored(OfficialResultSource.ADMIN_DIRECT, 80, 70);
        assertTrue(service.settle(command));

        verify(matchMapper).updateById(any());
        verify(matchPlayerStatMapper).softDeleteByMatchId(1L);
        verify(statsSettlementService).rebuildStageStats(10L);
        verify(matchSeriesService, never()).rebuildSeriesResult(any());
    }

    @Test
    void settle_sameOutcome_isIdempotent() {
        BasketV4Match match = scheduled();
        match.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        match.setHomeScore(80);
        match.setAwayScore(70);
        match.setWinnerEventTeamId(101L);
        when(matchMapper.selectByIdForUpdate(1L)).thenReturn(match);

        OfficialMatchResultCommand command = scored(OfficialResultSource.ADMIN_CORRECTION, 80, 70);
        command.setWinnerEventTeamId(101L);
        command.setLoserEventTeamId(102L);
        assertFalse(service.settle(command));
        verify(matchMapper, never()).updateById(any());
    }

    @Test
    void settle_adminDirectOnFinished_rejected() {
        BasketV4Match match = scheduled();
        match.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        match.setHomeScore(80);
        match.setAwayScore(70);
        match.setWinnerEventTeamId(101L);
        when(matchMapper.selectByIdForUpdate(1L)).thenReturn(match);

        OfficialMatchResultCommand command = scored(OfficialResultSource.ADMIN_DIRECT, 81, 70);
        assertThrows(ServiceException.class, () -> service.settle(command));
    }

    @Test
    void settle_forfeit_writesZeroScoreTeamStats() {
        BasketV4Match match = scheduled();
        when(matchMapper.selectByIdForUpdate(1L)).thenReturn(match);
        when(matchMapper.updateById(any())).thenReturn(1);

        OfficialMatchResultCommand command = new OfficialMatchResultCommand();
        command.setMatchId(1L);
        command.setWinnerEventTeamId(101L);
        command.setLoserEventTeamId(102L);
        command.setMatchStatus(MatchStatusEnum.FORFEITED.getCode());
        command.setSource(OfficialResultSource.FORFEIT);
        command.setClearOfficialPlayerStats(true);
        assertTrue(service.settle(command));

        ArgumentCaptor<BasketV4MatchTeamStat> captor = ArgumentCaptor.forClass(BasketV4MatchTeamStat.class);
        verify(matchTeamStatMapper, times(2)).insert(captor.capture());
        for (BasketV4MatchTeamStat row : captor.getAllValues()) {
            assertEquals(0, row.getScore());
            assertEquals(0, row.getPointsFor());
            assertEquals(0, row.getPointsAgainst());
        }
        verify(matchPlayerStatMapper).softDeleteByMatchId(1L);
        verify(statsSettlementService).rebuildStageStats(10L);
    }

    @Test
    void unsetOfficialResult_clearsForfeitAndRebuilds() {
        BasketV4Match match = scheduled();
        match.setMatchStatus(MatchStatusEnum.FORFEITED.getCode());
        match.setWinnerEventTeamId(101L);
        match.setLoserEventTeamId(102L);
        when(matchMapper.selectByIdForUpdate(1L)).thenReturn(match);
        when(matchMapper.clearOfficialResult(1L, MatchStatusEnum.SCHEDULED.getCode())).thenReturn(1);

        service.unsetOfficialResult(1L);

        verify(matchMapper).clearOfficialResult(1L, MatchStatusEnum.SCHEDULED.getCode());
        verify(matchPlayerStatMapper).softDeleteByMatchId(1L);
        verify(matchTeamStatMapper).softDeleteByMatchId(1L);
        verify(statsSettlementService).rebuildStageStats(10L);
    }

    @Test
    void unsetOfficialResult_rejectsNonForfeit() {
        when(matchMapper.selectByIdForUpdate(1L)).thenReturn(scheduled());
        assertThrows(ServiceException.class, () -> service.unsetOfficialResult(1L));
        verify(matchMapper, never()).clearOfficialResult(any(), any());
    }

    @Test
    void settle_childMatch_rebuildsSeries() {
        BasketV4Match match = scheduled();
        match.setSeriesId(20L);
        when(matchMapper.selectByIdForUpdate(1L)).thenReturn(match);
        when(matchMapper.updateById(any())).thenReturn(1);

        assertTrue(service.settle(scored(OfficialResultSource.ADMIN_DIRECT, 21, 15)));
        verify(matchSeriesService).rebuildSeriesResult(20L);
        verify(statsSettlementService).rebuildStageStats(10L);
    }

    private static BasketV4Match scheduled() {
        BasketV4Match match = new BasketV4Match();
        match.setId(1L);
        match.setStageId(10L);
        match.setHomeEventTeamId(101L);
        match.setAwayEventTeamId(102L);
        match.setMatchStatus(MatchStatusEnum.SCHEDULED.getCode());
        match.setPublishStatus("PUBLISHED");
        return match;
    }

    private static OfficialMatchResultCommand scored(OfficialResultSource source, int home, int away) {
        OfficialMatchResultCommand command = new OfficialMatchResultCommand();
        command.setMatchId(1L);
        command.setHomeScore(home);
        command.setAwayScore(away);
        command.setWinnerEventTeamId(home > away ? 101L : 102L);
        command.setLoserEventTeamId(home > away ? 102L : 101L);
        command.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        command.setSource(source);
        command.setClearOfficialPlayerStats(true);
        command.setScoreOnlyTeamStats(true);
        return command;
    }
}
