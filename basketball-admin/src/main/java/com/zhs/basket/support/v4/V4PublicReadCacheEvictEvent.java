package com.zhs.basket.support.v4;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class V4PublicReadCacheEvictEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long divisionId;
    private final Long matchId;
    private final Long seasonId;
    private final Long stageId;
    private final Long homeEventTeamId;
    private final Long awayEventTeamId;
    private final Long matchSeriesId;
    private final Long playerId;
    private final boolean evictSeriesList;
    private List<Long> retiredMatchIds = List.of();
    private List<Long> retiredSeriesIds = List.of();
    private List<Long> retiredStageIds = List.of();

    public V4PublicReadCacheEvictEvent(Long divisionId, Long matchId, Long seasonId, Long stageId,
                                       Long homeEventTeamId, Long awayEventTeamId) {
        this(divisionId, matchId, seasonId, stageId, homeEventTeamId, awayEventTeamId, null, null, false);
    }

    public V4PublicReadCacheEvictEvent(Long divisionId, Long matchId, Long seasonId, Long stageId,
                                       Long homeEventTeamId, Long awayEventTeamId, Long matchSeriesId) {
        this(divisionId, matchId, seasonId, stageId, homeEventTeamId, awayEventTeamId, matchSeriesId, null, false);
    }

    public V4PublicReadCacheEvictEvent(Long divisionId, Long matchId, Long seasonId, Long stageId,
                                       Long homeEventTeamId, Long awayEventTeamId, Long matchSeriesId,
                                       Long playerId, boolean evictSeriesList) {
        this.divisionId = divisionId;
        this.matchId = matchId;
        this.seasonId = seasonId;
        this.stageId = stageId;
        this.homeEventTeamId = homeEventTeamId;
        this.awayEventTeamId = awayEventTeamId;
        this.matchSeriesId = matchSeriesId;
        this.playerId = playerId;
        this.evictSeriesList = evictSeriesList;
    }

    public Long getDivisionId() { return divisionId; }
    public Long getMatchId() { return matchId; }
    public Long getSeasonId() { return seasonId; }
    public Long getStageId() { return stageId; }
    public Long getHomeEventTeamId() { return homeEventTeamId; }
    public Long getAwayEventTeamId() { return awayEventTeamId; }
    public Long getMatchSeriesId() { return matchSeriesId; }
    public Long getPlayerId() { return playerId; }
    public boolean isEvictSeriesList() { return evictSeriesList; }
    public List<Long> getRetiredMatchIds() { return retiredMatchIds; }
    public List<Long> getRetiredSeriesIds() { return retiredSeriesIds; }
    public List<Long> getRetiredStageIds() { return retiredStageIds; }
    public void setRetiredMatchIds(List<Long> retiredMatchIds) { this.retiredMatchIds = retiredMatchIds == null ? List.of() : retiredMatchIds; }
    public void setRetiredSeriesIds(List<Long> retiredSeriesIds) { this.retiredSeriesIds = retiredSeriesIds == null ? List.of() : retiredSeriesIds; }
    public void setRetiredStageIds(List<Long> retiredStageIds) { this.retiredStageIds = retiredStageIds == null ? List.of() : retiredStageIds; }
}