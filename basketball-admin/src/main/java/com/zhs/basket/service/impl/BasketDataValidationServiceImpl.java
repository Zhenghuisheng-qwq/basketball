package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.mapper.BasketConfirmedMatchAdminMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketTournamentRuleMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.query.MatchQuery;
import com.zhs.basket.service.IBasketDataValidationService;
import com.zhs.basket.support.StandingPointsRuleParser;
import com.zhs.basket.vo.ConfirmedMatchBriefVo;
import com.zhs.basket.vo.MatchListVo;
import com.zhs.basket.vo.SettlementValidationErrorVo;
import com.zhs.basket.vo.SettlementValidationResultVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Service
public class BasketDataValidationServiceImpl implements IBasketDataValidationService {

    @Autowired
    private BasketTournamentSeasonMapper seasonMapper;
    @Autowired
    private BasketTournamentRuleMapper ruleMapper;
    @Autowired
    private BasketMatchMapper matchMapper;
    @Autowired
    private BasketConfirmedMatchAdminMapper confirmedMatchAdminMapper;

    @Override
    public SettlementValidationResultVo validateSeason(Long seasonId) {
        BasketTournamentSeason season = requireSeason(seasonId);
        SettlementValidationResultVo result = new SettlementValidationResultVo();
        result.setSeasonId(season.getId());
        result.setUnfinishedMatchCount(confirmedMatchAdminMapper.countUnfinished(seasonId));
        result.setReopenedMatchCount(confirmedMatchAdminMapper.countReopened(seasonId));
        result.setCancelledMatchCount(confirmedMatchAdminMapper.countCancelled(seasonId));

        MatchQuery query = new MatchQuery();
        query.setSeasonId(seasonId);
        query.setMatchStatus(MatchStatusEnum.FINISHED.getCode());
        query.setDataStatus(MatchDataStatusEnum.CONFIRMED.getCode());
        List<MatchListVo> statusConfirmed = matchMapper.selectList(query);
        if (!CollectionUtils.isEmpty(statusConfirmed)) {
            for (MatchListVo match : statusConfirmed) {
                validateMatchStructure(match, result);
            }
        }

        List<ConfirmedMatchBriefVo> confirmed = confirmedMatchAdminMapper.selectConfirmedMatches(seasonId, null, null);
        result.setConfirmedMatchCount(confirmed == null ? 0 : confirmed.size());
        if (!CollectionUtils.isEmpty(confirmed)) {
            for (ConfirmedMatchBriefVo match : confirmed) {
                validateConfirmedMatch(match, result);
            }
        }

        boolean ruleOk = false;
        try {
            StandingPointsRuleParser.requireFromRules(ruleMapper.selectBySeasonId(seasonId));
            ruleOk = true;
        } catch (ServiceException ex) {
            result.getErrors().add(SettlementValidationErrorVo.of(null, null,
                    ex.getMessage() == null ? "积分规则未配置" : ex.getMessage()));
        }
        result.setStandingRuleConfigured(ruleOk);
        result.setErrorCount(result.getErrors().size());
        result.setPassed(result.getErrorCount() == 0);
        return result;
    }

    private void validateMatchStructure(MatchListVo match, SettlementValidationResultVo result) {
        Long matchId = match.getId();
        String matchNo = match.getMatchNo();
        if (match.getHomeTeamId() == null || match.getAwayTeamId() == null) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "确认比赛缺少主客队"));
            return;
        }
        if (Objects.equals(match.getHomeTeamId(), match.getAwayTeamId())) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "确认比赛主客队不能相同"));
        }
        if (match.getHomeScore() == null || match.getAwayScore() == null) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "确认比赛缺少比分"));
        } else if (match.getHomeScore() < 0 || match.getAwayScore() < 0) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "确认比赛比分非法"));
        }
    }

    private void validateConfirmedMatch(ConfirmedMatchBriefVo match, SettlementValidationResultVo result) {
        Long matchId = match.getMatchId();
        String matchNo = match.getMatchNo();
        if (match.getHomeTeamId() == null || match.getAwayTeamId() == null) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "确认比赛缺少主客队"));
            return;
        }
        if (!isWinnerConsistent(match)) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "比分与胜负不一致"));
        }
        // 弃赛/判负场次仅写入 20:0 正式结果，不要求完整技术统计
        if (match.getWalkoverTeamId() != null) {
            return;
        }

        int teamStatCount = confirmedMatchAdminMapper.countConfirmedTeamStat(matchId);
        if (teamStatCount < 2) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "缺少已确认的球队技术统计"));
        }
        int playerStatCount = confirmedMatchAdminMapper.countConfirmedPlayerStat(matchId);
        if (playerStatCount <= 0) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "缺少已确认的球员技术统计"));
        }

        Integer homePlayerPts = confirmedMatchAdminMapper.sumPlayerPointsByMatchTeam(matchId, match.getHomeTeamId());
        Integer awayPlayerPts = confirmedMatchAdminMapper.sumPlayerPointsByMatchTeam(matchId, match.getAwayTeamId());
        if (!Objects.equals(nvl(homePlayerPts), nvl(match.getHomeScore()))
                || !Objects.equals(nvl(awayPlayerPts), nvl(match.getAwayScore()))) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "球员得分合计与比赛比分不一致"));
        }

        Integer homeTeamPts = confirmedMatchAdminMapper.sumTeamPointsByMatchTeam(matchId, match.getHomeTeamId());
        Integer awayTeamPts = confirmedMatchAdminMapper.sumTeamPointsByMatchTeam(matchId, match.getAwayTeamId());
        if (!Objects.equals(nvl(homeTeamPts), nvl(match.getHomeScore()))
                || !Objects.equals(nvl(awayTeamPts), nvl(match.getAwayScore()))) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "球队统计得分与比赛比分不一致"));
        }

        if (confirmedMatchAdminMapper.countDuplicateTeamStat(matchId) > 0) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "存在重复的球队技术统计"));
        }
        if (confirmedMatchAdminMapper.countDuplicatePlayerStat(matchId) > 0) {
            result.getErrors().add(SettlementValidationErrorVo.of(matchId, matchNo, "存在重复的球员技术统计"));
        }
    }

    private boolean isWinnerConsistent(ConfirmedMatchBriefVo match) {
        int home = nvl(match.getHomeScore());
        int away = nvl(match.getAwayScore());
        Long winner = match.getWinnerTeamId();
        if (home > away) {
            return winner != null && winner.equals(match.getHomeTeamId());
        }
        if (away > home) {
            return winner != null && winner.equals(match.getAwayTeamId());
        }
        return winner == null;
    }

    private BasketTournamentSeason requireSeason(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季ID不能为空");
        }
        BasketTournamentSeason season = seasonMapper.selectById(seasonId);
        if (season == null || (season.getIsDelete() != null && season.getIsDelete() != 0)) {
            throw new ServiceException("赛季不存在");
        }
        return season;
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }
}
