package com.zhs.service.v4;

import com.zhs.response.v4.publicapi.*;

import java.util.List;

public interface IBasketV4PublicService {

    List<V4PublicSeriesItemResp> listSeries();

    List<V4PublicSeasonItemResp> listSeasons(String seriesCode, Integer seasonYear, String seasonStatus);

    V4PublicSeasonItemResp getCurrentSeason(String seriesCode);

    V4PublicSeasonDetailResp getSeasonDetail(Long seasonId);

    List<V4PublicDivisionListItemResp> listDivisions(Long seasonId);

    V4PublicDivisionDetailResp getDivisionDetail(Long divisionId);

    List<V4PublicEventTeamItemResp> listTeams(Long divisionId);

    V4PublicEventTeamDetailResp getTeamDetail(Long eventTeamId);

    V4PublicTeamArchiveResp getTeamArchive(Long eventTeamId);

    List<V4PublicMatchItemResp> listMatches(Long divisionId, Integer roundNo, String matchStatus,
                                            Boolean upcomingOnly, Boolean finishedOnly, Long eventTeamId);

    V4PublicMatchDetailResp getMatchDetail(Long matchId);

    List<V4PublicStandingItemResp> listStanding(Long divisionId);

    List<V4PublicStageItemResp> listStages(Long divisionId);

    List<V4PublicStageStandingScopeResp> listStageStandings(Long stageId);

    List<V4PublicMatchItemResp> listStageBracket(Long stageId);

    List<V4PublicMatchSeriesDetailResp> listStageSeries(Long stageId);

    V4PublicMatchSeriesDetailResp getMatchSeriesDetail(Long seriesId);

    List<V4PublicScheduleItemResp> listDivisionSchedule(Long divisionId);

    List<V4PublicTeamStatItemResp> listTeamStat(Long divisionId);

    List<V4PublicPlayerStatItemResp> listPlayerStat(Long divisionId);

    List<V4PublicRankItemResp> listRank(Long divisionId, String rankType);
}
