package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.v4.BasketCaptainGrantDao;
import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketEventPlayerDao;
import com.zhs.dao.v4.BasketEventTeamDao;
import com.zhs.dao.v4.BasketMatchSeriesDao;
import com.zhs.dao.v4.BasketMatchSeriesSubmissionDao;
import com.zhs.dao.v4.BasketMatchSubmissionDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.dao.v4.BasketV4MatchDao;
import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.enums.v4.MatchPublishStatusEnum;
import com.zhs.enums.v4.MatchStatusEnum;
import com.zhs.enums.v4.SubmissionStatusEnum;
import com.zhs.enums.v4.SubmitterRoleEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.v4.BasketCaptainGrant;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketEventPlayer;
import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketMatchSeries;
import com.zhs.model.v4.BasketMatchSeriesSubmission;
import com.zhs.model.v4.BasketMatchSubmission;
import com.zhs.model.v4.BasketSeason;
import com.zhs.model.v4.BasketV4Match;
import com.zhs.request.v4.V4SeriesSubmissionDeclareReq;
import com.zhs.request.v4.V4SeriesSubmissionIdReq;
import com.zhs.request.v4.V4SeriesSubmissionResubmitReq;
import com.zhs.request.v4.V4SeriesSubmissionSaveGameReq;
import com.zhs.response.v4.V4MatchSubmissionEligiblePlayerResp;
import com.zhs.response.v4.V4SeriesSubmissionGameResp;
import com.zhs.response.v4.V4SeriesSubmissionWorkspaceResp;
import com.zhs.service.v4.IBasketV4PlayerSeriesSubmissionService;
import com.zhs.support.v4.EventTeamCaptainAuthorization;
import com.zhs.support.v4.PublicCurrentRosterSupport;
import com.zhs.support.v4.FullMatchSubmissionRules;
import com.zhs.support.v4.FullMatchSubmissionSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class BasketV4PlayerSeriesSubmissionServiceImpl implements IBasketV4PlayerSeriesSubmissionService {

    private static final Integer NOT_DELETED = 0;
    private static final String EVENT_PLAYER_STATUS_ACTIVE = "ACTIVE";

    @Resource
    private BasketMatchSeriesDao matchSeriesDao;
    @Resource
    private BasketV4MatchDao matchDao;
    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private BasketEventPlayerDao eventPlayerDao;
    @Resource
    private BasketCaptainGrantDao captainGrantDao;
    @Resource
    private BasketMatchSubmissionDao submissionDao;
    @Resource
    private BasketMatchSeriesSubmissionDao seriesSubmissionDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private EventTeamCaptainAuthorization eventTeamCaptainAuthorization;
    @Resource
    private FullMatchSubmissionSupport fullMatchSubmissionSupport;
    @Resource
    private PublicCurrentRosterSupport currentRosterSupport;
    @Resource
    private com.zhs.support.v4.EventTeamParticipationGuard eventTeamParticipationGuard;

    @Override
    public V4SeriesSubmissionWorkspaceResp getWorkspace(Long matchSeriesId) {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        BasketMatchSeries series = requireSeries(matchSeriesId);
        SideContext side = requireSeriesSideCaptain(series, player.getId(), memberId);
        BasketMatchSeriesSubmission header = findLatestHeader(series.getId());
        return buildWorkspace(series, header, side);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long declareScore(V4SeriesSubmissionDeclareReq req) {
        if (req == null || req.getMatchSeriesId() == null
                || req.getDeclaredHomeWinCount() == null || req.getDeclaredAwayWinCount() == null) {
            throw new ServiceException("系列赛比分不能为空");
        }
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        BasketMatchSeries series = requireSeries(req.getMatchSeriesId());
        divisionDao.lockById(series.getDivisionId());
        SideContext side = requireSeriesSideCaptain(series, player.getId(), memberId);
        FullMatchSubmissionRules.assertValidSeriesScore(nvl(series.getBestOf()),
                req.getDeclaredHomeWinCount(), req.getDeclaredAwayWinCount());
        Date now = new Date();
        BasketMatchSeriesSubmission header = requireEditableHeader(series, side, memberId, now);
        header.setDeclaredHomeWinCount(req.getDeclaredHomeWinCount());
        header.setDeclaredAwayWinCount(req.getDeclaredAwayWinCount());
        header.setActualGameCount(FullMatchSubmissionRules.actualGameCount(
                nvl(series.getBestOf()), req.getDeclaredHomeWinCount(), req.getDeclaredAwayWinCount()));
        header.setUpdateTime(now);
        seriesSubmissionDao.updateById(header);
        return header.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveGame(V4SeriesSubmissionSaveGameReq req) {
        if (req == null || req.getMatchSeriesId() == null || req.getMatchId() == null) {
            throw new ServiceException("matchSeriesId / matchId 不能为空");
        }
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        BasketMatchSeries series = requireSeries(req.getMatchSeriesId());
        divisionDao.lockById(series.getDivisionId());
        SideContext side = requireSeriesSideCaptain(series, player.getId(), memberId);
        BasketV4Match match = requirePublishedMatch(req.getMatchId());
        if (!series.getId().equals(match.getSeriesId())) {
            throw new ServiceException("比赛不属于该系列赛");
        }
        if ("COMPLETED".equals(series.getSeriesStatus())
                && !MatchStatusEnum.FINISHED.getCode().equals(match.getMatchStatus())
                && !MatchStatusEnum.FORFEITED.getCode().equals(match.getMatchStatus())) {
            throw new ServiceException("该系列赛已决出胜负，本局无需进行");
        }
        Date now = new Date();
        BasketMatchSeriesSubmission header = requireEditableHeader(series, side, memberId, now);
        if (req.getDeclaredHomeWinCount() != null && req.getDeclaredAwayWinCount() != null) {
            FullMatchSubmissionRules.assertValidSeriesScore(nvl(series.getBestOf()),
                    req.getDeclaredHomeWinCount(), req.getDeclaredAwayWinCount());
            header.setDeclaredHomeWinCount(req.getDeclaredHomeWinCount());
            header.setDeclaredAwayWinCount(req.getDeclaredAwayWinCount());
            header.setActualGameCount(FullMatchSubmissionRules.actualGameCount(
                    nvl(series.getBestOf()), req.getDeclaredHomeWinCount(), req.getDeclaredAwayWinCount()));
        }
        int actual = header.getActualGameCount() == null ? nvl(series.getBestOf()) : header.getActualGameCount();
        int gameNo = nvl(match.getSeriesGameNo());
        if (gameNo > actual) {
            throw new ServiceException("G" + gameNo + " 未发生，无需填写");
        }
        boolean complete = Boolean.TRUE.equals(req.getDraftComplete());
        String gameLabel = "G" + gameNo;
        BasketMatchSubmission submission = findOrCreateGameSubmission(match, header, side, memberId, now);
        int homeScore = req.getHomeScore() != null ? req.getHomeScore() : FullMatchSubmissionRules.sumPoints(req.getHomePlayerStats());
        int awayScore = req.getAwayScore() != null ? req.getAwayScore() : FullMatchSubmissionRules.sumPoints(req.getAwayPlayerStats());
        submission.setHomeScore(homeScore);
        submission.setAwayScore(awayScore);
        submission.setSubmitRemark(req.getSubmitRemark());
        submission.setSubmissionMode(FullMatchSubmissionRules.MODE_WINNER_FULL_MATCH);
        submission.setSeriesSubmissionId(header.getId());
        submission.setDraftComplete(complete ? 1 : 0);
        submission.setSubmissionStatus(SubmissionStatusEnum.DRAFT.getCode());
        submission.setUpdateTime(now);
        fullMatchSubmissionSupport.replaceFullMatchPayload(submission, match,
                req.getHomePlayerStats(), req.getAwayPlayerStats(), req.getPeriodScores(),
                req.getMaterialFileIds(), complete, gameLabel, now);
        submissionDao.updateById(submission);
        header.setUpdateTime(now);
        seriesSubmissionDao.updateById(header);
        return submission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSeries(V4SeriesSubmissionIdReq req) {
        if (req == null || (req.getSeriesSubmissionId() == null && req.getMatchSeriesId() == null)) {
            throw new ServiceException("seriesSubmissionId 不能为空");
        }
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        BasketMatchSeriesSubmission header = req.getSeriesSubmissionId() != null
                ? seriesSubmissionDao.selectByIdForUpdate(req.getSeriesSubmissionId())
                : findLatestHeader(req.getMatchSeriesId());
        if (header == null || !NOT_DELETED.equals(header.getIsDelete())) {
            throw new ServiceException("系列赛提交任务不存在");
        }
        BasketMatchSeries series = requireSeries(header.getMatchSeriesId());
        divisionDao.lockById(series.getDivisionId());
        SideContext side = requireSeriesSideCaptain(series, player.getId(), memberId);
        if (!SubmissionStatusEnum.DRAFT.getCode().equals(header.getStatus())
                && !SubmissionStatusEnum.REJECTED.getCode().equals(header.getStatus())) {
            throw new ServiceException("当前状态不可提交整个系列赛审核");
        }
        if (header.getDeclaredHomeWinCount() == null || header.getDeclaredAwayWinCount() == null) {
            throw new ServiceException("请先确认系列赛最终比分");
        }
        FullMatchSubmissionRules.assertValidSeriesScore(nvl(series.getBestOf()),
                header.getDeclaredHomeWinCount(), header.getDeclaredAwayWinCount());
        int actual = FullMatchSubmissionRules.actualGameCount(nvl(series.getBestOf()),
                header.getDeclaredHomeWinCount(), header.getDeclaredAwayWinCount());
        List<BasketV4Match> games = listSeriesGames(series.getId());
        int homeWins = 0;
        int awayWins = 0;
        int completed = 0;
        for (BasketV4Match match : games) {
            int gameNo = nvl(match.getSeriesGameNo());
            if (gameNo > actual) {
                continue;
            }
            BasketMatchSubmission submission = findLatestGameSubmission(match.getId(), header.getId());
            if (submission == null || !Integer.valueOf(1).equals(submission.getDraftComplete())) {
                throw new ServiceException("G" + gameNo + " 尚未完成，不能提交整个系列赛审核");
            }
            fullMatchSubmissionSupport.validatePersistedComplete(submission, match, "G" + gameNo);
            if (nvl(submission.getHomeScore()) > nvl(submission.getAwayScore())) {
                homeWins++;
            } else if (nvl(submission.getAwayScore()) > nvl(submission.getHomeScore())) {
                awayWins++;
            }
            completed++;
        }
        if (completed != actual) {
            throw new ServiceException("实际完成场次 " + completed + " 与声明场次 " + actual + " 不一致");
        }
        if (homeWins != header.getDeclaredHomeWinCount() || awayWins != header.getDeclaredAwayWinCount()) {
            throw new ServiceException("各 Game 胜负累计 " + homeWins + ":" + awayWins
                    + " 必须等于声明的系列赛比分 "
                    + header.getDeclaredHomeWinCount() + ":" + header.getDeclaredAwayWinCount());
        }
        Date now = new Date();
        header.setSubmitEventTeamId(side.eventTeamId);
        header.setSubmitMemberId(memberId);
        header.setActualGameCount(actual);
        header.setStatus(SubmissionStatusEnum.PENDING_REVIEW.getCode());
        header.setSubmitTime(now);
        header.setUpdateTime(now);
        seriesSubmissionDao.updateById(header);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long resubmitSeries(V4SeriesSubmissionResubmitReq req) {
        if (req == null || req.getOldSeriesSubmissionId() == null) {
            throw new ServiceException("oldSeriesSubmissionId 不能为空");
        }
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        BasketMatchSeriesSubmission old = seriesSubmissionDao.selectById(req.getOldSeriesSubmissionId());
        if (old == null || !NOT_DELETED.equals(old.getIsDelete())) {
            throw new ServiceException("原系列赛提交不存在");
        }
        if (!SubmissionStatusEnum.REJECTED.getCode().equals(old.getStatus())) {
            throw new ServiceException("仅驳回状态可重提");
        }
        BasketMatchSeries series = requireSeries(old.getMatchSeriesId());
        divisionDao.lockById(series.getDivisionId());
        SideContext side = requireSeriesSideCaptain(series, player.getId(), memberId);
        Date now = new Date();
        old.setStatus(SubmissionStatusEnum.SUPERSEDED.getCode());
        old.setUpdateTime(now);
        seriesSubmissionDao.updateById(old);

        BasketMatchSeriesSubmission header = new BasketMatchSeriesSubmission();
        header.setMatchSeriesId(series.getId());
        header.setSeasonId(old.getSeasonId());
        header.setDivisionId(series.getDivisionId());
        header.setStageId(series.getStageId());
        header.setSubmitEventTeamId(side.eventTeamId);
        header.setSubmitMemberId(memberId);
        header.setDeclaredHomeWinCount(req.getDeclaredHomeWinCount() != null
                ? req.getDeclaredHomeWinCount() : old.getDeclaredHomeWinCount());
        header.setDeclaredAwayWinCount(req.getDeclaredAwayWinCount() != null
                ? req.getDeclaredAwayWinCount() : old.getDeclaredAwayWinCount());
        if (header.getDeclaredHomeWinCount() != null && header.getDeclaredAwayWinCount() != null) {
            FullMatchSubmissionRules.assertValidSeriesScore(nvl(series.getBestOf()),
                    header.getDeclaredHomeWinCount(), header.getDeclaredAwayWinCount());
            header.setActualGameCount(FullMatchSubmissionRules.actualGameCount(
                    nvl(series.getBestOf()), header.getDeclaredHomeWinCount(), header.getDeclaredAwayWinCount()));
        }
        header.setStatus(SubmissionStatusEnum.DRAFT.getCode());
        header.setVersionNo(nvl(old.getVersionNo()) + 1);
        header.setCreateTime(now);
        header.setUpdateTime(now);
        header.setIsDelete(NOT_DELETED);
        seriesSubmissionDao.insert(header);

        for (BasketMatchSubmission game : listHeaderGames(old.getId())) {
            game.setSeriesSubmissionId(header.getId());
            game.setSubmissionStatus(SubmissionStatusEnum.DRAFT.getCode());
            game.setUpdateTime(now);
            submissionDao.updateById(game);
        }
        return header.getId();
    }

    private V4SeriesSubmissionWorkspaceResp buildWorkspace(BasketMatchSeries series,
                                                           BasketMatchSeriesSubmission header,
                                                           SideContext side) {
        V4SeriesSubmissionWorkspaceResp resp = new V4SeriesSubmissionWorkspaceResp();
        resp.setMatchSeriesId(series.getId());
        resp.setSeriesCode(series.getSeriesCode());
        resp.setBestOf(series.getBestOf());
        resp.setRoundNo(series.getRoundNo());
        resp.setHomeEventTeamId(series.getHomeEventTeamId());
        resp.setAwayEventTeamId(series.getAwayEventTeamId());
        resp.setHomeTeamName(teamName(series.getHomeEventTeamId()));
        resp.setAwayTeamName(teamName(series.getAwayEventTeamId()));
        fillDivision(resp, series);
        resp.setHomeEligiblePlayers(loadEligiblePlayers(series.getHomeEventTeamId()));
        resp.setAwayEligiblePlayers(loadEligiblePlayers(series.getAwayEventTeamId()));
        if (header != null) {
            resp.setSeriesSubmissionId(header.getId());
            resp.setVersionNo(header.getVersionNo());
            resp.setStatus(header.getStatus());
            resp.setDeclaredHomeWinCount(header.getDeclaredHomeWinCount());
            resp.setDeclaredAwayWinCount(header.getDeclaredAwayWinCount());
            resp.setActualGameCount(header.getActualGameCount());
            resp.setSubmitEventTeamId(header.getSubmitEventTeamId());
            resp.setSubmitTeamName(teamName(header.getSubmitEventTeamId()));
            resp.setSubmitTime(header.getSubmitTime());
            resp.setRejectReason(header.getRejectReason());
            resp.setAuditRemark(header.getAuditRemark());
        } else {
            resp.setStatus(SubmissionStatusEnum.DRAFT.getCode());
            resp.setVersionNo(1);
        }
        String status = resp.getStatus();
        boolean editable = SubmissionStatusEnum.DRAFT.getCode().equals(status)
                || SubmissionStatusEnum.REJECTED.getCode().equals(status);
        resp.setCanEdit(editable);
        int actual = resp.getActualGameCount() == null ? nvl(series.getBestOf()) : resp.getActualGameCount();
        List<V4SeriesSubmissionGameResp> games = new ArrayList<>();
        int completed = 0;
        for (BasketV4Match match : listSeriesGames(series.getId())) {
            V4SeriesSubmissionGameResp game = new V4SeriesSubmissionGameResp();
            game.setMatchId(match.getId());
            game.setSeriesGameNo(match.getSeriesGameNo());
            game.setMatchNo(match.getMatchNo());
            boolean required = nvl(match.getSeriesGameNo()) <= actual;
            game.setRequired(required);
            game.setSkipped(!required);
            BasketMatchSubmission submission = header == null ? null : findLatestGameSubmission(match.getId(), header.getId());
            if (submission != null) {
                game.setSubmissionId(submission.getId());
                game.setSubmissionStatus(submission.getSubmissionStatus());
                game.setDraftComplete(submission.getDraftComplete());
                game.setHomeScore(submission.getHomeScore());
                game.setAwayScore(submission.getAwayScore());
                game.setSubmitRemark(submission.getSubmitRemark());
                fullMatchSubmissionSupport.fillGame(game, submission, match);
                if (Integer.valueOf(1).equals(submission.getDraftComplete()) && required) {
                    completed++;
                }
            }
            games.add(game);
        }
        resp.setGames(games);
        resp.setCompletedGameCount(completed);
        resp.setCanSubmitSeries(editable
                && resp.getDeclaredHomeWinCount() != null
                && resp.getDeclaredAwayWinCount() != null
                && completed == actual
                && actual > 0);
        return resp;
    }

    private BasketMatchSeriesSubmission requireEditableHeader(BasketMatchSeries series, SideContext side,
                                                              Long memberId, Date now) {
        BasketMatchSeriesSubmission header = findLatestHeader(series.getId());
        if (header == null) {
            header = new BasketMatchSeriesSubmission();
            header.setMatchSeriesId(series.getId());
            header.setSeasonId(firstSeasonId(series.getId()));
            header.setDivisionId(series.getDivisionId());
            header.setStageId(series.getStageId());
            header.setSubmitEventTeamId(side.eventTeamId);
            header.setSubmitMemberId(memberId);
            header.setStatus(SubmissionStatusEnum.DRAFT.getCode());
            header.setVersionNo(1);
            header.setCreateTime(now);
            header.setUpdateTime(now);
            header.setIsDelete(NOT_DELETED);
            seriesSubmissionDao.insert(header);
            return header;
        }
        if (SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(header.getStatus())) {
            throw new ServiceException("系列赛审核中不可覆盖，请等待审核结果");
        }
        if (SubmissionStatusEnum.APPROVED.getCode().equals(header.getStatus())) {
            throw new ServiceException("已通过的系列赛提交不可编辑");
        }
        header.setSubmitEventTeamId(side.eventTeamId);
        header.setSubmitMemberId(memberId);
        return header;
    }

    private BasketMatchSubmission findOrCreateGameSubmission(BasketV4Match match, BasketMatchSeriesSubmission header,
                                                             SideContext side, Long memberId, Date now) {
        BasketMatchSubmission latest = findLatestGameSubmission(match.getId(), header.getId());
        if (latest != null) {
            if (SubmissionStatusEnum.APPROVED.getCode().equals(latest.getSubmissionStatus())
                    || SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(latest.getSubmissionStatus())) {
                throw new ServiceException("当前对局不可覆盖保存");
            }
            return latest;
        }
        BasketMatchSubmission submission = new BasketMatchSubmission();
        submission.setMatchId(match.getId());
        submission.setSeasonId(match.getSeasonId());
        submission.setDivisionId(match.getDivisionId());
        submission.setSubmitEventTeamId(side.eventTeamId);
        submission.setOpponentEventTeamId(side.eventTeamId.equals(match.getHomeEventTeamId())
                ? match.getAwayEventTeamId() : match.getHomeEventTeamId());
        submission.setSubmitMemberId(memberId);
        submission.setSubmitterRole(SubmitterRoleEnum.CAPTAIN.getCode());
        submission.setSubmissionStatus(SubmissionStatusEnum.DRAFT.getCode());
        submission.setVersionNo(1);
        submission.setSubmissionMode(FullMatchSubmissionRules.MODE_WINNER_FULL_MATCH);
        submission.setSeriesSubmissionId(header.getId());
        submission.setDraftComplete(0);
        submission.setCreateTime(now);
        submission.setUpdateTime(now);
        submission.setIsDelete(NOT_DELETED);
        submissionDao.insert(submission);
        return submission;
    }

    private BasketMatchSeriesSubmission findLatestHeader(Long matchSeriesId) {
        return seriesSubmissionDao.selectOne(new LambdaQueryWrapper<BasketMatchSeriesSubmission>()
                .eq(BasketMatchSeriesSubmission::getMatchSeriesId, matchSeriesId)
                .eq(BasketMatchSeriesSubmission::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchSeriesSubmission::getVersionNo)
                .last("limit 1"));
    }

    private BasketMatchSubmission findLatestGameSubmission(Long matchId, Long seriesSubmissionId) {
        return submissionDao.selectOne(new LambdaQueryWrapper<BasketMatchSubmission>()
                .eq(BasketMatchSubmission::getMatchId, matchId)
                .eq(BasketMatchSubmission::getSeriesSubmissionId, seriesSubmissionId)
                .eq(BasketMatchSubmission::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchSubmission::getVersionNo)
                .last("limit 1"));
    }

    private List<BasketMatchSubmission> listHeaderGames(Long seriesSubmissionId) {
        return submissionDao.selectList(new LambdaQueryWrapper<BasketMatchSubmission>()
                .eq(BasketMatchSubmission::getSeriesSubmissionId, seriesSubmissionId)
                .eq(BasketMatchSubmission::getIsDelete, NOT_DELETED));
    }

    private List<BasketV4Match> listSeriesGames(Long seriesId) {
        List<BasketV4Match> matches = matchDao.selectList(new LambdaQueryWrapper<BasketV4Match>()
                .eq(BasketV4Match::getSeriesId, seriesId)
                .eq(BasketV4Match::getIsDelete, NOT_DELETED));
        matches.sort(Comparator.comparing(m -> nvl(m.getSeriesGameNo())));
        return matches;
    }

    private SideContext requireSeriesSideCaptain(BasketMatchSeries series, Long playerId, Long memberId) {
        FullMatchSubmissionRules.assertPairingResolved(series.getHomeEventTeamId(), series.getAwayEventTeamId());
        if (isAuthorizedCaptain(series.getDivisionId(), series.getHomeEventTeamId(), playerId)
                || belongsToEventTeam(series.getHomeEventTeamId(), playerId, memberId)
                && isAuthorizedCaptain(series.getDivisionId(), series.getHomeEventTeamId(), playerId)) {
            eventTeamParticipationGuard.assertCanParticipate(series.getHomeEventTeamId());
            SideContext ctx = new SideContext();
            ctx.eventTeamId = series.getHomeEventTeamId();
            return ctx;
        }
        if (isAuthorizedCaptain(series.getDivisionId(), series.getAwayEventTeamId(), playerId)) {
            eventTeamParticipationGuard.assertCanParticipate(series.getAwayEventTeamId());
            SideContext ctx = new SideContext();
            ctx.eventTeamId = series.getAwayEventTeamId();
            return ctx;
        }
        throw new ServiceException("仅本系列赛主客队认证队长可编辑系列赛数据");
    }

    private boolean belongsToEventTeam(Long eventTeamId, Long playerId, Long memberId) {
        return eventPlayerDao.selectOne(new LambdaQueryWrapper<BasketEventPlayer>()
                .eq(BasketEventPlayer::getEventTeamId, eventTeamId)
                .eq(BasketEventPlayer::getIsDelete, NOT_DELETED)
                .and(w -> w.eq(BasketEventPlayer::getSourcePlayerId, playerId).or().eq(BasketEventPlayer::getMemberId, memberId))
                .last("limit 1")) != null;
    }

    private boolean isAuthorizedCaptain(Long divisionId, Long eventTeamId, Long playerId) {
        BasketEventTeam eventTeam = eventTeamDao.selectById(eventTeamId);
        if (eventTeam == null) {
            return false;
        }
        BasketCaptainGrant grant = captainGrantDao.selectOne(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getDivisionId, divisionId)
                .eq(BasketCaptainGrant::getPlayerId, playerId)
                .eq(BasketCaptainGrant::getGrantStatus, CaptainGrantStatusEnum.ACTIVE.getCode())
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNotNull(BasketCaptainGrant::getActiveUniqueKey)
                .last("limit 1"));
        return eventTeamCaptainAuthorization.canSubmitAsCaptain(eventTeam, playerId, grant);
    }

    private List<V4MatchSubmissionEligiblePlayerResp> loadEligiblePlayers(Long eventTeamId) {
        List<BasketEventPlayer> players = eventPlayerDao.selectList(new LambdaQueryWrapper<BasketEventPlayer>()
                .eq(BasketEventPlayer::getEventTeamId, eventTeamId)
                .eq(BasketEventPlayer::getIsDelete, NOT_DELETED)
                .and(w -> w.eq(BasketEventPlayer::getEventPlayerStatus, EVENT_PLAYER_STATUS_ACTIVE)
                        .or().isNull(BasketEventPlayer::getEventPlayerStatus))
                .orderByDesc(BasketEventPlayer::getIsCaptain)
                .orderByDesc(BasketEventPlayer::getIsStarter)
                .orderByAsc(BasketEventPlayer::getJerseyNo)
                .orderByAsc(BasketEventPlayer::getId));
        List<V4MatchSubmissionEligiblePlayerResp> result = new ArrayList<>();
        for (BasketEventPlayer player : players) {
            V4MatchSubmissionEligiblePlayerResp item = new V4MatchSubmissionEligiblePlayerResp();
            item.setEventPlayerId(player.getId());
            item.setPlayerName(player.getPlayerNameSnapshot());
            item.setGameId(player.getGameIdSnapshot());
            item.setJerseyNo(player.getJerseyNo());
            item.setPosition(player.getPosition());
            item.setIsCaptain(player.getIsCaptain());
            result.add(item);
        }
        currentRosterSupport.applyEligibleNames(players, result);
        return result;
    }

    private void fillDivision(V4SeriesSubmissionWorkspaceResp resp, BasketMatchSeries series) {
        if (series.getDivisionId() != null) {
            BasketDivision division = divisionDao.selectById(series.getDivisionId());
            if (division != null) {
                resp.setDivisionName(division.getDivisionName());
                resp.setGameMode(division.getGameMode());
                if (division.getSeasonId() != null) {
                    BasketSeason season = seasonDao.selectById(division.getSeasonId());
                    if (season != null) {
                        resp.setSeasonTitle(season.getDisplayTitle() != null ? season.getDisplayTitle() : season.getSeasonName());
                    }
                }
            }
        }
    }

    private Long firstSeasonId(Long seriesId) {
        return listSeriesGames(seriesId).stream().map(BasketV4Match::getSeasonId).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String teamName(Long eventTeamId) {
        if (eventTeamId == null) {
            return null;
        }
        BasketEventTeam team = eventTeamDao.selectById(eventTeamId);
        return team == null ? null : team.getTeamNameSnapshot();
    }

    private BasketMatchSeries requireSeries(Long matchSeriesId) {
        BasketMatchSeries series = matchSeriesDao.selectById(matchSeriesId);
        if (series == null || !NOT_DELETED.equals(series.getIsDelete())) {
            throw new ServiceException("系列赛不存在");
        }
        if (nvl(series.getBestOf()) <= 1) {
            throw new ServiceException("BO1 请使用单场比赛数据提交");
        }
        return series;
    }

    private BasketV4Match requirePublishedMatch(Long matchId) {
        BasketV4Match match = matchDao.selectById(matchId);
        if (match == null || !NOT_DELETED.equals(match.getIsDelete())) {
            throw new ServiceException("比赛不存在");
        }
        if (!MatchPublishStatusEnum.PUBLISHED.getCode().equals(match.getPublishStatus())) {
            throw new ServiceException("仅已发布比赛可提交");
        }
        if (MatchStatusEnum.CANCELLED.getCode().equals(match.getMatchStatus())) {
            throw new ServiceException("已取消比赛不可提交");
        }
        return match;
    }

    private BasketPlayerProfile requirePlayerByMemberId(Long memberId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            throw new ServiceException("当前账号未关联认证球员");
        }
        return profile;
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private static class SideContext {
        private Long eventTeamId;
    }
}
