package com.zhs.support.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketEventPlayerDao;
import com.zhs.dao.v4.BasketMatchSubmissionMaterialDao;
import com.zhs.dao.v4.BasketMatchSubmissionPeriodScoreDao;
import com.zhs.dao.v4.BasketMatchSubmissionPlayerStatDao;
import com.zhs.dao.v4.BasketMatchSubmissionTeamStatDao;
import com.zhs.exception.ServiceException;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketEventPlayer;
import com.zhs.model.v4.BasketMatchSubmission;
import com.zhs.model.v4.BasketMatchSubmissionMaterial;
import com.zhs.model.v4.BasketMatchSubmissionPeriodScore;
import com.zhs.model.v4.BasketMatchSubmissionPlayerStat;
import com.zhs.model.v4.BasketMatchSubmissionTeamStat;
import com.zhs.model.v4.BasketV4Match;
import com.zhs.request.v4.V4MatchSubmissionPeriodScoreReq;
import com.zhs.request.v4.V4MatchSubmissionPlayerStatReq;
import com.zhs.request.v4.V4MatchSubmissionSaveReq;
import com.zhs.response.v4.V4MatchSubmissionDetailResp;
import com.zhs.response.v4.V4MatchSubmissionPeriodScoreResp;
import com.zhs.response.v4.V4MatchSubmissionPlayerStatResp;
import com.zhs.response.v4.V4SeriesSubmissionGameResp;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class FullMatchSubmissionSupport {

    private static final Integer NOT_DELETED = 0;
    private static final String EVENT_PLAYER_STATUS_ACTIVE = "ACTIVE";

    @Resource
    private BasketMatchSubmissionTeamStatDao submissionTeamStatDao;
    @Resource
    private BasketMatchSubmissionPlayerStatDao submissionPlayerStatDao;
    @Resource
    private BasketMatchSubmissionMaterialDao submissionMaterialDao;
    @Resource
    private BasketMatchSubmissionPeriodScoreDao submissionPeriodScoreDao;
    @Resource
    private BasketEventPlayerDao eventPlayerDao;
    @Resource
    private BasketDivisionDao divisionDao;

    public boolean isFullMatchReq(V4MatchSubmissionSaveReq req) {
        if (req == null) {
            return false;
        }
        if (FullMatchSubmissionRules.isWinnerFullMatch(req.getSubmissionMode())) {
            return true;
        }
        return !CollectionUtils.isEmpty(req.getHomePlayerStats())
                || !CollectionUtils.isEmpty(req.getAwayPlayerStats())
                || !CollectionUtils.isEmpty(req.getPeriodScores());
    }

    public boolean isFullMatch(BasketMatchSubmission submission) {
        return submission != null && FullMatchSubmissionRules.isWinnerFullMatch(submission.getSubmissionMode());
    }

    public void replaceFullMatchPayload(BasketMatchSubmission submission, BasketV4Match match,
                                        List<V4MatchSubmissionPlayerStatReq> homePlayers,
                                        List<V4MatchSubmissionPlayerStatReq> awayPlayers,
                                        List<V4MatchSubmissionPeriodScoreReq> periodScores,
                                        List<Long> materialFileIds,
                                        boolean requireComplete,
                                        String gameLabel,
                                        Date now) {
        String homeName = "主队";
        String awayName = "客队";
        FullMatchSubmissionRules.validateTeamPlayers(gameLabel, homeName, match.getHomeEventTeamId(), homePlayers, requireComplete);
        FullMatchSubmissionRules.validateTeamPlayers(gameLabel, awayName, match.getAwayEventTeamId(), awayPlayers, requireComplete);
        assertPlayersBelongToTeam(gameLabel, homeName, match.getHomeEventTeamId(), homePlayers);
        assertPlayersBelongToTeam(gameLabel, awayName, match.getAwayEventTeamId(), awayPlayers);

        int homePts = FullMatchSubmissionRules.sumPoints(homePlayers);
        int awayPts = FullMatchSubmissionRules.sumPoints(awayPlayers);
        int homeScore = submission.getHomeScore() != null ? submission.getHomeScore() : homePts;
        int awayScore = submission.getAwayScore() != null ? submission.getAwayScore() : awayPts;
        submission.setHomeScore(homeScore);
        submission.setAwayScore(awayScore);
        boolean requirePeriods = FullMatchSubmissionRules.requiresPeriodScores(resolveGameMode(match));
        if (requireComplete) {
            FullMatchSubmissionRules.validateScoreCross(gameLabel, homeScore, awayScore, homePts, awayPts);
            FullMatchSubmissionRules.validatePeriodScores(gameLabel, periodScores, homeScore, awayScore, true, requirePeriods);
            FullMatchSubmissionRules.validateMaterials(gameLabel, materialFileIds, true);
        } else {
            FullMatchSubmissionRules.validatePeriodScores(gameLabel, periodScores, homeScore, awayScore, false, requirePeriods);
            FullMatchSubmissionRules.validateMaterials(gameLabel, materialFileIds, false);
        }

        clearPayload(submission.getId());
        insertTeamStat(submission, match.getHomeEventTeamId(), homePlayers, now);
        insertTeamStat(submission, match.getAwayEventTeamId(), awayPlayers, now);
        insertPlayers(submission, match.getHomeEventTeamId(), homePlayers, now);
        insertPlayers(submission, match.getAwayEventTeamId(), awayPlayers, now);
        if (requirePeriods) {
            insertPeriods(submission, periodScores, now);
        }
        insertMaterials(submission, materialFileIds, now);
    }

    public void validatePersistedComplete(BasketMatchSubmission submission, BasketV4Match match, String gameLabel) {
        List<V4MatchSubmissionPlayerStatReq> home = toReqs(loadPlayers(submission.getId(), match.getHomeEventTeamId()));
        List<V4MatchSubmissionPlayerStatReq> away = toReqs(loadPlayers(submission.getId(), match.getAwayEventTeamId()));
        List<V4MatchSubmissionPeriodScoreReq> periods = toPeriodReqs(loadPeriods(submission.getId()));
        List<Long> materials = loadMaterialIds(submission.getId());
        FullMatchSubmissionRules.validateTeamPlayers(gameLabel, "主队", match.getHomeEventTeamId(), home, true);
        FullMatchSubmissionRules.validateTeamPlayers(gameLabel, "客队", match.getAwayEventTeamId(), away, true);
        int homePts = FullMatchSubmissionRules.sumPoints(home);
        int awayPts = FullMatchSubmissionRules.sumPoints(away);
        int homeScore = FullMatchSubmissionRules.nvl(submission.getHomeScore());
        int awayScore = FullMatchSubmissionRules.nvl(submission.getAwayScore());
        FullMatchSubmissionRules.validateScoreCross(gameLabel, homeScore, awayScore, homePts, awayPts);
        boolean requirePeriods = FullMatchSubmissionRules.requiresPeriodScores(resolveGameMode(match));
        FullMatchSubmissionRules.validatePeriodScores(gameLabel, periods, homeScore, awayScore, true, requirePeriods);
        FullMatchSubmissionRules.validateMaterials(gameLabel, materials, true);
    }

    public String resolveGameMode(BasketV4Match match) {
        if (match == null || match.getDivisionId() == null) {
            return "FIVE_V_FIVE";
        }
        BasketDivision division = divisionDao.selectById(match.getDivisionId());
        if (division == null || division.getGameMode() == null || division.getGameMode().isBlank()) {
            return "FIVE_V_FIVE";
        }
        return division.getGameMode();
    }

    public void fillDetail(V4MatchSubmissionDetailResp resp, BasketMatchSubmission submission, BasketV4Match match) {
        fillGame(toGameHolder(resp, submission, match), submission, match);
    }

    public void fillGame(V4SeriesSubmissionGameResp game, BasketMatchSubmission submission, BasketV4Match match) {
        if (submission == null) {
            return;
        }
        List<BasketMatchSubmissionTeamStat> teamStats = submissionTeamStatDao.selectList(
                new LambdaQueryWrapper<BasketMatchSubmissionTeamStat>()
                        .eq(BasketMatchSubmissionTeamStat::getSubmissionId, submission.getId())
                        .eq(BasketMatchSubmissionTeamStat::getIsDelete, NOT_DELETED));
        for (BasketMatchSubmissionTeamStat ts : teamStats) {
            if (ts.getEventTeamId().equals(match.getHomeEventTeamId())) {
                game.setHomeTeamStat(TeamStatAggregator.toResp(ts));
            } else if (ts.getEventTeamId().equals(match.getAwayEventTeamId())) {
                game.setAwayTeamStat(TeamStatAggregator.toResp(ts));
            }
        }
        game.setHomePlayerStats(toPlayerResps(loadPlayers(submission.getId(), match.getHomeEventTeamId())));
        game.setAwayPlayerStats(toPlayerResps(loadPlayers(submission.getId(), match.getAwayEventTeamId())));
        game.setPeriodScores(toPeriodResps(loadPeriods(submission.getId())));
        game.setMaterialFileIds(loadMaterialIds(submission.getId()));
    }

    public List<BasketMatchSubmissionPlayerStat> loadPlayers(Long submissionId, Long eventTeamId) {
        return submissionPlayerStatDao.selectList(new LambdaQueryWrapper<BasketMatchSubmissionPlayerStat>()
                .eq(BasketMatchSubmissionPlayerStat::getSubmissionId, submissionId)
                .eq(eventTeamId != null, BasketMatchSubmissionPlayerStat::getEventTeamId, eventTeamId)
                .eq(BasketMatchSubmissionPlayerStat::getIsDelete, NOT_DELETED)
                .orderByAsc(BasketMatchSubmissionPlayerStat::getId));
    }

    public List<BasketMatchSubmissionPeriodScore> loadPeriods(Long submissionId) {
        return submissionPeriodScoreDao.selectList(new LambdaQueryWrapper<BasketMatchSubmissionPeriodScore>()
                .eq(BasketMatchSubmissionPeriodScore::getSubmissionId, submissionId)
                .eq(BasketMatchSubmissionPeriodScore::getIsDelete, NOT_DELETED)
                .orderByAsc(BasketMatchSubmissionPeriodScore::getPeriodNo));
    }

    public List<Long> loadMaterialIds(Long submissionId) {
        return submissionMaterialDao.selectList(new LambdaQueryWrapper<BasketMatchSubmissionMaterial>()
                        .eq(BasketMatchSubmissionMaterial::getSubmissionId, submissionId)
                        .eq(BasketMatchSubmissionMaterial::getIsDelete, NOT_DELETED))
                .stream().map(BasketMatchSubmissionMaterial::getFileId).toList();
    }

    private void clearPayload(Long submissionId) {
        submissionTeamStatDao.delete(new LambdaQueryWrapper<BasketMatchSubmissionTeamStat>()
                .eq(BasketMatchSubmissionTeamStat::getSubmissionId, submissionId));
        submissionPlayerStatDao.delete(new LambdaQueryWrapper<BasketMatchSubmissionPlayerStat>()
                .eq(BasketMatchSubmissionPlayerStat::getSubmissionId, submissionId));
        submissionMaterialDao.delete(new LambdaQueryWrapper<BasketMatchSubmissionMaterial>()
                .eq(BasketMatchSubmissionMaterial::getSubmissionId, submissionId));
        submissionPeriodScoreDao.delete(new LambdaQueryWrapper<BasketMatchSubmissionPeriodScore>()
                .eq(BasketMatchSubmissionPeriodScore::getSubmissionId, submissionId));
    }

    private void insertTeamStat(BasketMatchSubmission submission, Long eventTeamId,
                                List<V4MatchSubmissionPlayerStatReq> rows, Date now) {
        BasketMatchSubmissionTeamStat ts = TeamStatAggregator.aggregate(rows);
        ts.setSubmissionId(submission.getId());
        ts.setMatchId(submission.getMatchId());
        ts.setDivisionId(submission.getDivisionId());
        ts.setEventTeamId(eventTeamId);
        ts.setCreateTime(now);
        ts.setUpdateTime(now);
        ts.setIsDelete(NOT_DELETED);
        submissionTeamStatDao.insert(ts);
    }

    private void insertPlayers(BasketMatchSubmission submission, Long eventTeamId,
                               List<V4MatchSubmissionPlayerStatReq> rows, Date now) {
        for (V4MatchSubmissionPlayerStatReq req : FullMatchSubmissionRules.usedRows(rows)) {
            if (req.getEventPlayerId() == null) {
                continue;
            }
            BasketMatchSubmissionPlayerStat ps = new BasketMatchSubmissionPlayerStat();
            BeanUtils.copyProperties(req, ps);
            ps.setId(null);
            ps.setSubmissionId(submission.getId());
            ps.setMatchId(submission.getMatchId());
            ps.setDivisionId(submission.getDivisionId());
            ps.setEventTeamId(eventTeamId);
            ps.setCreateTime(now);
            ps.setUpdateTime(now);
            ps.setIsDelete(NOT_DELETED);
            submissionPlayerStatDao.insert(ps);
        }
    }

    private void insertPeriods(BasketMatchSubmission submission, List<V4MatchSubmissionPeriodScoreReq> periods, Date now) {
        if (CollectionUtils.isEmpty(periods)) {
            return;
        }
        for (V4MatchSubmissionPeriodScoreReq req : periods) {
            if (req == null || req.getPeriodNo() == null) {
                continue;
            }
            BasketMatchSubmissionPeriodScore row = new BasketMatchSubmissionPeriodScore();
            row.setSubmissionId(submission.getId());
            row.setMatchId(submission.getMatchId());
            row.setDivisionId(submission.getDivisionId());
            row.setPeriodNo(req.getPeriodNo());
            row.setHomeScore(FullMatchSubmissionRules.nvl(req.getHomeScore()));
            row.setAwayScore(FullMatchSubmissionRules.nvl(req.getAwayScore()));
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            submissionPeriodScoreDao.insert(row);
        }
    }

    private void insertMaterials(BasketMatchSubmission submission, List<Long> materialFileIds, Date now) {
        if (CollectionUtils.isEmpty(materialFileIds)) {
            return;
        }
        for (Long fileId : materialFileIds) {
            if (fileId == null) {
                continue;
            }
            BasketMatchSubmissionMaterial m = new BasketMatchSubmissionMaterial();
            m.setSubmissionId(submission.getId());
            m.setMatchId(submission.getMatchId());
            m.setDivisionId(submission.getDivisionId());
            m.setMaterialType("IMAGE");
            m.setFileId(fileId);
            m.setCreateTime(now);
            m.setUpdateTime(now);
            m.setIsDelete(NOT_DELETED);
            submissionMaterialDao.insert(m);
        }
    }

    private void assertPlayersBelongToTeam(String gameLabel, String teamName, Long eventTeamId,
                                           List<V4MatchSubmissionPlayerStatReq> rows) {
        for (V4MatchSubmissionPlayerStatReq row : FullMatchSubmissionRules.usedRows(rows)) {
            if (row.getEventPlayerId() == null) {
                continue;
            }
            BasketEventPlayer ep = eventPlayerDao.selectById(row.getEventPlayerId());
            if (ep == null || !NOT_DELETED.equals(ep.getIsDelete()) || !eventTeamId.equals(ep.getEventTeamId())) {
                throw new ServiceException(FullMatchSubmissionRules.formatPrefixForTeam(gameLabel, teamName)
                        + "：球员不属于该队当前有效名单");
            }
            if (ep.getEventPlayerStatus() != null
                    && !EVENT_PLAYER_STATUS_ACTIVE.equals(ep.getEventPlayerStatus())) {
                throw new ServiceException(FullMatchSubmissionRules.formatPrefixForTeam(gameLabel, teamName)
                        + "：球员当前无效");
            }
        }
    }

    private List<V4MatchSubmissionPlayerStatReq> toReqs(List<BasketMatchSubmissionPlayerStat> rows) {
        List<V4MatchSubmissionPlayerStatReq> reqs = new ArrayList<>();
        for (BasketMatchSubmissionPlayerStat row : rows) {
            V4MatchSubmissionPlayerStatReq req = new V4MatchSubmissionPlayerStatReq();
            BeanUtils.copyProperties(row, req);
            reqs.add(req);
        }
        return reqs;
    }

    private List<V4MatchSubmissionPeriodScoreReq> toPeriodReqs(List<BasketMatchSubmissionPeriodScore> rows) {
        List<V4MatchSubmissionPeriodScoreReq> reqs = new ArrayList<>();
        for (BasketMatchSubmissionPeriodScore row : rows) {
            V4MatchSubmissionPeriodScoreReq req = new V4MatchSubmissionPeriodScoreReq();
            req.setPeriodNo(row.getPeriodNo());
            req.setHomeScore(row.getHomeScore());
            req.setAwayScore(row.getAwayScore());
            reqs.add(req);
        }
        return reqs;
    }

    private List<V4MatchSubmissionPlayerStatResp> toPlayerResps(List<BasketMatchSubmissionPlayerStat> rows) {
        List<V4MatchSubmissionPlayerStatResp> resps = new ArrayList<>();
        for (BasketMatchSubmissionPlayerStat row : rows) {
            V4MatchSubmissionPlayerStatResp resp = new V4MatchSubmissionPlayerStatResp();
            BeanUtils.copyProperties(row, resp);
            BasketEventPlayer ep = eventPlayerDao.selectById(row.getEventPlayerId());
            if (ep != null) {
                resp.setPlayerName(ep.getPlayerNameSnapshot());
            }
            resps.add(resp);
        }
        return resps;
    }

    private List<V4MatchSubmissionPeriodScoreResp> toPeriodResps(List<BasketMatchSubmissionPeriodScore> rows) {
        List<V4MatchSubmissionPeriodScoreResp> resps = new ArrayList<>();
        for (BasketMatchSubmissionPeriodScore row : rows) {
            V4MatchSubmissionPeriodScoreResp resp = new V4MatchSubmissionPeriodScoreResp();
            resp.setPeriodNo(row.getPeriodNo());
            resp.setHomeScore(row.getHomeScore());
            resp.setAwayScore(row.getAwayScore());
            resps.add(resp);
        }
        return resps;
    }

    private V4SeriesSubmissionGameResp toGameHolder(V4MatchSubmissionDetailResp resp,
                                                    BasketMatchSubmission submission,
                                                    BasketV4Match match) {
        V4SeriesSubmissionGameResp game = new V4SeriesSubmissionGameResp();
        fillGame(game, submission, match);
        resp.setHomeTeamStat(game.getHomeTeamStat());
        resp.setAwayTeamStat(game.getAwayTeamStat());
        resp.setHomePlayerStats(game.getHomePlayerStats());
        resp.setAwayPlayerStats(game.getAwayPlayerStats());
        resp.setPeriodScores(game.getPeriodScores());
        if (resp.getMaterialFileIds() == null || resp.getMaterialFileIds().isEmpty()) {
            resp.setMaterialFileIds(game.getMaterialFileIds());
        }
        return game;
    }
}
