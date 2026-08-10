package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketEventPlayerDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketPlayerStatDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.enums.v4.PublishStatusEnum;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketEventPlayer;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketPlayerStat;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketSeries;
import com.zhs.response.publicapi.PublicPlayerSeasonStatVo;
import com.zhs.service.support.BasketPublicSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketPublicPlayerSeasonStatTest {

    @Mock
    private BasketEventPlayerDao eventPlayerDao;
    @Mock
    private BasketEventTeamDao eventTeamDao;
    @Mock
    private BasketPlayerStatDao playerStatDao;
    @Mock
    private BasketSeasonDao seasonDao;
    @Mock
    private BasketDivisionDao divisionDao;
    @Mock
    private BasketPublicSupport publicSupport;

    @InjectMocks
    private BasketPublicPlayerServiceImpl service;

    @Test
    void listSeasonStat_returnsDivisionScopedRowsWithNullShooting() {
        Long sourcePlayerId = 2084958459421265922L;

        BasketPlayerProfile profile = new BasketPlayerProfile();
        profile.setId(sourcePlayerId);
        when(publicSupport.requirePlayer(sourcePlayerId)).thenReturn(profile);
        when(publicSupport.isPlayerPubliclyVisible(profile)).thenReturn(true);
        when(publicSupport.canShowMatchData(profile)).thenReturn(true);

        BasketEventPlayer epPc = eventPlayer(576L, sourcePlayerId, 102L, 137L, 135L, "PC-Player");
        BasketEventPlayer epConsole = eventPlayer(566L, sourcePlayerId, 90L, 130L, 133L, "Console-Player");
        when(eventPlayerDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(epPc, epConsole));

        BasketPlayerStat statPc = playerStat(576L, 102L, 137L, 135L, 2, 40, 10, 8);
        BasketPlayerStat statConsole = playerStat(566L, 90L, 130L, 133L, 1, 12, 4, 3);
        when(playerStatDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(statPc, statConsole));

        BasketEventTeam teamPc = eventTeam(135L, "Team PC");
        BasketEventTeam teamConsole = eventTeam(133L, "Team Console");
        when(eventTeamDao.selectByIds(anyCollection())).thenReturn(List.of(teamPc, teamConsole));

        BasketDivision divPc = division(137L, "PC 5V5", "PC", "FIVE_V_FIVE");
        BasketDivision divConsole = division(130L, "PS/Xbox 5V5", "PS_XBOX", "FIVE_V_FIVE");
        when(divisionDao.selectByIds(anyCollection())).thenReturn(List.of(divPc, divConsole));

        BasketSeason seasonNew = season(102L, 1L, "CNPA League 2026 S21", daysAgo(1));
        BasketSeason seasonOld = season(90L, 1L, "SMOKE OLD", daysAgo(10));
        when(publicSupport.loadSeasonMap(anyCollection())).thenReturn(Map.of(
                102L, seasonNew,
                90L, seasonOld
        ));

        BasketSeries series = new BasketSeries();
        series.setId(1L);
        series.setSeriesName("CNPA League");
        when(publicSupport.loadSeriesMap(anyCollection())).thenReturn(Map.of(1L, series));
        when(publicSupport.loadAccessUrlMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(publicSupport.loadPlayerAvatarUrlMap(any())).thenReturn(Collections.emptyMap());

        List<PublicPlayerSeasonStatVo> rows = service.listSeasonStat(sourcePlayerId, null);

        assertEquals(2, rows.size());
        assertEquals(137L, rows.get(0).getDivisionId());
        assertEquals(130L, rows.get(1).getDivisionId());
        assertEquals(sourcePlayerId, rows.get(0).getSourcePlayerId());
        assertEquals(576L, rows.get(0).getEventPlayerId());
        assertEquals(135L, rows.get(0).getEventTeamId());
        assertEquals(new BigDecimal("20.00"), rows.get(0).getAvgPoints());
        assertNull(rows.get(0).getFieldGoalPct());
        assertNull(rows.get(0).getThreePointPct());
        assertNull(rows.get(0).getFreeThrowPct());
        assertTrue(rows.stream().map(PublicPlayerSeasonStatVo::getDivisionId).distinct().count() == 2);
    }

    @Test
    void listSeasonStat_keepsFinishedDivisionHistory() {
        Long sourcePlayerId = 100L;
        BasketPlayerProfile profile = new BasketPlayerProfile();
        profile.setId(sourcePlayerId);
        when(publicSupport.requirePlayer(sourcePlayerId)).thenReturn(profile);
        when(publicSupport.isPlayerPubliclyVisible(profile)).thenReturn(true);
        when(publicSupport.canShowMatchData(profile)).thenReturn(true);

        BasketEventPlayer ep = eventPlayer(1L, sourcePlayerId, 10L, 20L, 30L, "Finished Player");
        when(eventPlayerDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ep));

        BasketPlayerStat stat = playerStat(1L, 10L, 20L, 30L, 3, 30, 9, 6);
        when(playerStatDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(stat));
        when(eventTeamDao.selectByIds(anyCollection())).thenReturn(List.of(eventTeam(30L, "Archived Snapshot")));
        BasketDivision finished = division(20L, "Finished Div", "PC", "FIVE_V_FIVE");
        finished.setDivisionStatus("FINISHED");
        when(divisionDao.selectByIds(anyCollection())).thenReturn(List.of(finished));

        BasketSeason season = season(10L, 2L, "Finished Season", daysAgo(30));
        when(publicSupport.loadSeasonMap(anyCollection())).thenReturn(Map.of(10L, season));
        when(publicSupport.loadSeriesMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(publicSupport.loadAccessUrlMap(anyCollection())).thenReturn(Collections.emptyMap());
        when(publicSupport.loadPlayerAvatarUrlMap(any())).thenReturn(Collections.emptyMap());

        List<PublicPlayerSeasonStatVo> rows = service.listSeasonStat(sourcePlayerId, null);
        assertEquals(1, rows.size());
        assertEquals("FINISHED", finished.getDivisionStatus());
        assertEquals("Archived Snapshot", rows.get(0).getTeamName());
        assertNull(rows.get(0).getFieldGoalPct());
    }

    private static BasketEventPlayer eventPlayer(Long id, Long sourcePlayerId, Long seasonId, Long divisionId,
                                                 Long eventTeamId, String name) {
        BasketEventPlayer ep = new BasketEventPlayer();
        ep.setId(id);
        ep.setSourcePlayerId(sourcePlayerId);
        ep.setSeasonId(seasonId);
        ep.setDivisionId(divisionId);
        ep.setEventTeamId(eventTeamId);
        ep.setPlayerNameSnapshot(name);
        ep.setIsDelete(0);
        return ep;
    }

    private static BasketPlayerStat playerStat(Long eventPlayerId, Long seasonId, Long divisionId, Long eventTeamId,
                                               int games, int points, int rebound, int assist) {
        BasketPlayerStat stat = new BasketPlayerStat();
        stat.setEventPlayerId(eventPlayerId);
        stat.setSeasonId(seasonId);
        stat.setDivisionId(divisionId);
        stat.setEventTeamId(eventTeamId);
        stat.setMatchCount(games);
        stat.setPoints(points);
        stat.setAvgPoints(BigDecimal.valueOf(points).divide(BigDecimal.valueOf(games), 2, java.math.RoundingMode.HALF_UP));
        stat.setRebound(rebound);
        stat.setAssist(assist);
        stat.setSteal(0);
        stat.setBlock(0);
        stat.setTurnover(0);
        stat.setIsDelete(0);
        return stat;
    }

    private static BasketEventTeam eventTeam(Long id, String name) {
        BasketEventTeam team = new BasketEventTeam();
        team.setId(id);
        team.setTeamNameSnapshot(name);
        team.setIsDelete(0);
        return team;
    }

    private static BasketDivision division(Long id, String name, String platform, String mode) {
        BasketDivision division = new BasketDivision();
        division.setId(id);
        division.setDivisionName(name);
        division.setPlatformCode(platform);
        division.setGameMode(mode);
        division.setIsDelete(0);
        return division;
    }

    private static BasketSeason season(Long id, Long seriesId, String title, Date matchStart) {
        BasketSeason season = new BasketSeason();
        season.setId(id);
        season.setSeriesId(seriesId);
        season.setDisplayTitle(title);
        season.setSeasonName(title);
        season.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        season.setMatchStartTime(matchStart);
        season.setIsDelete(0);
        return season;
    }

    private static Date daysAgo(int days) {
        return new Date(System.currentTimeMillis() - days * 24L * 3600_000L);
    }
}
