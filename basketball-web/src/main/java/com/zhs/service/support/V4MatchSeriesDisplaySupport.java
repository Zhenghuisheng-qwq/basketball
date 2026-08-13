package com.zhs.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketV4MatchDao;
import com.zhs.enums.v4.MatchStatusEnum;
import com.zhs.mapper.v4.BasketV4MatchSeriesMapper;
import com.zhs.mapper.v4.BasketV4PublicStageMapper;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketMatchSeries;
import com.zhs.model.v4.BasketV4Match;
import com.zhs.response.v4.publicapi.V4PublicMatchSeriesDetailResp;
import com.zhs.response.v4.publicapi.V4PublicMatchSeriesGameResp;
import com.zhs.response.v4.publicapi.V4PublicStageItemResp;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
import java.util.*;

@Component
public class V4MatchSeriesDisplaySupport {

    @Resource
    private BasketV4MatchSeriesMapper matchSeriesMapper;
    @Resource
    private BasketV4MatchDao matchDao;
    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private BasketV4PublicStageMapper publicStageMapper;

    public V4PublicMatchSeriesDetailResp buildDetail(Long seriesId, Long currentMatchId) {
        if (seriesId == null) {
            return null;
        }
        BasketMatchSeries series = matchSeriesMapper.selectById(seriesId);
        if (series == null) {
            return null;
        }
        return buildDetail(series, currentMatchId);
    }

    public List<V4PublicMatchSeriesDetailResp> listByStageId(Long stageId) {
        List<BasketMatchSeries> list = matchSeriesMapper.selectByStageId(stageId);
        if (CollectionUtils.isEmpty(list)) {
            return List.of();
        }
        List<V4PublicMatchSeriesDetailResp> result = new ArrayList<>();
        for (BasketMatchSeries series : list) {
            result.add(buildDetail(series, null));
        }
        return result;
    }

    public V4PublicMatchSeriesDetailResp buildDetail(BasketMatchSeries series, Long currentMatchId) {
        V4PublicMatchSeriesDetailResp vo = new V4PublicMatchSeriesDetailResp();
        vo.setId(series.getId());
        vo.setSeriesCode(series.getSeriesCode());
        vo.setDivisionId(series.getDivisionId());
        vo.setStageId(series.getStageId());
        vo.setRoundNo(series.getRoundNo());
        vo.setBestOf(series.getBestOf());
        vo.setRequiredWins(requiredWins(series.getBestOf()));
        vo.setHomeWinCount(series.getHomeWinCount());
        vo.setAwayWinCount(series.getAwayWinCount());
        vo.setSeriesStatus(series.getSeriesStatus());
        vo.setSeriesStatusName(seriesStatusLabel(series.getSeriesStatus()));
        vo.setResolutionType(series.getResolutionType());
        vo.setResolutionTypeName(resolutionTypeLabel(series.getResolutionType()));
        vo.setWinnerEventTeamId(series.getWinnerEventTeamId());
        vo.setHomeEventTeamId(series.getHomeEventTeamId());
        vo.setAwayEventTeamId(series.getAwayEventTeamId());
        vo.setHomeSourceType(series.getHomeSourceType());
        vo.setAwaySourceType(series.getAwaySourceType());
        vo.setHomeSourceDisplay(series.getHomeSourceDisplay());
        vo.setAwaySourceDisplay(series.getAwaySourceDisplay());

        fillStageInfo(vo, series);
        fillTeamNames(vo, series);
        fillForfeitSummary(vo, series);
        vo.setGames(buildGames(series, currentMatchId));
        return vo;
    }

    private void fillStageInfo(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series) {
        if (series.getDivisionId() == null || series.getStageId() == null) {
            return;
        }
        List<V4PublicStageItemResp> stages = publicStageMapper.selectStagesByDivisionId(series.getDivisionId());
        if (CollectionUtils.isEmpty(stages)) {
            return;
        }
        for (V4PublicStageItemResp stage : stages) {
            if (series.getStageId().equals(stage.getStageId())) {
                vo.setStageName(stage.getStageName());
                boolean finalStage = (stage.getNextStageCode() == null || stage.getNextStageCode().isEmpty())
                        && "NONE".equalsIgnoreCase(String.valueOf(stage.getAdvancementType()));
                vo.setFinalStage(finalStage);
                if ("COMPLETED".equals(series.getSeriesStatus()) && series.getWinnerEventTeamId() != null) {
                    vo.setResultLabel(Boolean.TRUE.equals(vo.getFinalStage()) ? "冠军" : "本轮胜者");
                }
                break;
            }
        }
    }

    private void fillTeamNames(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series) {
        Set<Long> ids = new HashSet<>();
        if (series.getHomeEventTeamId() != null) ids.add(series.getHomeEventTeamId());
        if (series.getAwayEventTeamId() != null) ids.add(series.getAwayEventTeamId());
        if (series.getWinnerEventTeamId() != null) ids.add(series.getWinnerEventTeamId());
        if (ids.isEmpty()) {
            if (series.getHomeSourceDisplay() != null && !series.getHomeSourceDisplay().isBlank()) {
                vo.setHomeTeamName(series.getHomeSourceDisplay());
            } else {
                vo.setHomeTeamName("待定");
            }
            if (series.getAwaySourceDisplay() != null && !series.getAwaySourceDisplay().isBlank()) {
                vo.setAwayTeamName(series.getAwaySourceDisplay());
            } else {
                vo.setAwayTeamName("待定");
            }
            return;
        }
        List<BasketEventTeam> teams = eventTeamDao.selectByIds(ids);
        Map<Long, BasketEventTeam> map = new HashMap<>();
        for (BasketEventTeam team : teams) {
            if (team != null) {
                map.put(team.getId(), team);
            }
        }
        BasketEventTeam home = map.get(series.getHomeEventTeamId());
        if (home != null) {
            vo.setHomeTeamName(home.getTeamNameSnapshot());
        } else if (series.getHomeSourceDisplay() != null && !series.getHomeSourceDisplay().isBlank()) {
            vo.setHomeTeamName(series.getHomeSourceDisplay());
        } else if (series.getHomeEventTeamId() == null) {
            vo.setHomeTeamName("待定");
        }
        BasketEventTeam away = map.get(series.getAwayEventTeamId());
        if (away != null) {
            vo.setAwayTeamName(away.getTeamNameSnapshot());
        } else if (series.getAwaySourceDisplay() != null && !series.getAwaySourceDisplay().isBlank()) {
            vo.setAwayTeamName(series.getAwaySourceDisplay());
        } else if (series.getAwayEventTeamId() == null) {
            vo.setAwayTeamName("待定");
        }
        BasketEventTeam winner = map.get(series.getWinnerEventTeamId());
        if (winner != null) {
            vo.setWinnerTeamName(winner.getTeamNameSnapshot());
        }
    }

    private List<V4PublicMatchSeriesGameResp> buildGames(BasketMatchSeries series, Long currentMatchId) {
        List<BasketV4Match> matches = matchDao.selectList(new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getSeriesId, series.getId())
                .eq(BasketV4Match::getIsDelete, BasketPublicConstants.NOT_DELETED)
                .orderByAsc(BasketV4Match::getSeriesGameNo));
        if (CollectionUtils.isEmpty(matches)) {
            return List.of();
        }
        boolean seriesCompleted = "COMPLETED".equals(series.getSeriesStatus());
        boolean seriesForfeit = "FORFEIT".equalsIgnoreCase(String.valueOf(series.getResolutionType()));
        List<V4PublicMatchSeriesGameResp> games = new ArrayList<>();
        for (BasketV4Match match : matches) {
            V4PublicMatchSeriesGameResp game = new V4PublicMatchSeriesGameResp();
            game.setMatchId(match.getId());
            game.setMatchCode(match.getMatchNo());
            game.setSeriesGameNo(match.getSeriesGameNo());
            game.setMatchStatus(match.getMatchStatus());
            game.setMatchStatusName(matchStatusLabel(match.getMatchStatus()));
            game.setHomeScore(match.getHomeScore());
            game.setAwayScore(match.getAwayScore());
            game.setWinnerEventTeamId(match.getWinnerEventTeamId());
            game.setScheduledTime(match.getMatchTime());
            game.setCurrentGame(currentMatchId != null && currentMatchId.equals(match.getId()));
            if ((seriesCompleted || seriesForfeit) && !isOfficialResult(match)) {
                game.setSkipped(true);
                game.setSkippedReason(seriesForfeit ? "系列赛已判定弃赛，本局无需进行" : "系列赛已提前结束，本局无需进行");
            }
            games.add(game);
        }
        return games;
    }

    public Map<Long, BasketMatchSeries> loadSeriesMap(Collection<Long> seriesIds) {
        if (CollectionUtils.isEmpty(seriesIds)) {
            return Collections.emptyMap();
        }
        Map<Long, BasketMatchSeries> map = new HashMap<>();
        for (Long id : seriesIds) {
            if (id == null) continue;
            BasketMatchSeries series = matchSeriesMapper.selectById(id);
            if (series != null) {
                map.put(id, series);
            }
        }
        return map;
    }

    private boolean isOfficialResult(BasketV4Match match) {
        return MatchStatusEnum.FINISHED.getCode().equals(match.getMatchStatus())
                || MatchStatusEnum.FORFEITED.getCode().equals(match.getMatchStatus());
    }

    private int requiredWins(Integer bestOf) {
        if (bestOf == null || bestOf <= 1) {
            return 1;
        }
        return bestOf / 2 + 1;
    }

    private void fillForfeitSummary(V4PublicMatchSeriesDetailResp vo, BasketMatchSeries series) {
        if (!"FORFEIT".equalsIgnoreCase(String.valueOf(series.getResolutionType()))) {
            return;
        }
        if (vo.getWinnerTeamName() == null) {
            return;
        }
        String loserName = null;
        if (series.getWinnerEventTeamId() != null) {
            if (series.getWinnerEventTeamId().equals(series.getHomeEventTeamId())) {
                loserName = vo.getAwayTeamName();
            } else if (series.getWinnerEventTeamId().equals(series.getAwayEventTeamId())) {
                loserName = vo.getHomeTeamName();
            }
        }
        if (loserName != null) {
            vo.setResultSummary(loserName + " 弃赛，" + vo.getWinnerTeamName() + " 晋级");
        } else {
            vo.setResultSummary("系列赛弃赛，" + vo.getWinnerTeamName() + " 晋级");
        }
    }

    private String resolutionTypeLabel(String type) {
        if (type == null || type.isEmpty()) {
            return "正常结束";
        }
        return switch (type.toUpperCase()) {
            case "FORFEIT" -> "系列赛弃赛";
            case "MANUAL" -> "人工裁定";
            case "NORMAL" -> "正常结束";
            default -> type;
        };
    }

    private String seriesStatusLabel(String status) {
        if (status == null) {
            return "--";
        }
        return switch (status) {
            case "PENDING" -> "待准备";
            case "READY" -> "待比赛";
            case "RUNNING" -> "进行中";
            case "COMPLETED" -> "已结束";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    private String matchStatusLabel(String status) {
        if (status == null) {
            return "--";
        }
        try {
            return MatchStatusEnum.fromCode(status).getDescription();
        } catch (Exception ex) {
            return status;
        }
    }
}
