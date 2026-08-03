package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.v4.*;
import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.enums.v4.MatchPublishStatusEnum;
import com.zhs.enums.v4.MatchStatusEnum;
import com.zhs.enums.v4.SubmissionStatusEnum;
import com.zhs.enums.v4.SubmitterRoleEnum;
import com.zhs.exception.ServiceException;
import com.zhs.mapper.v4.BasketV4PlayerMatchSubmissionMapper;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.v4.*;
import com.zhs.request.v4.*;
import com.zhs.response.v4.*;
import com.zhs.service.v4.IBasketV4PlayerMatchSubmissionService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class BasketV4PlayerMatchSubmissionServiceImpl implements IBasketV4PlayerMatchSubmissionService {

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketV4PlayerMatchSubmissionMapper playerMatchSubmissionMapper;
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
    private BasketMatchSubmissionTeamStatDao submissionTeamStatDao;
    @Resource
    private BasketMatchSubmissionPlayerStatDao submissionPlayerStatDao;
    @Resource
    private BasketMatchSubmissionMaterialDao submissionMaterialDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;

    @Override
    public List<V4MyMatchResp> listMyMatches(Long seasonId, Long divisionId, String matchStatus,
                                             String publishStatus, String submissionStatus, Boolean upcomingOnly) {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        List<V4MyMatchResp> rows = playerMatchSubmissionMapper.selectMyMatches(
                memberId, player.getId(), seasonId, divisionId, matchStatus, publishStatus, submissionStatus, upcomingOnly);
        for (V4MyMatchResp row : rows) {
            enrichCanSubmit(row, player.getId());
        }
        return rows;
    }

    @Override
    public V4MatchSubmissionDetailResp getDetail(Long matchId, Long submitEventTeamId) {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        BasketV4Match match = requireMatch(matchId);
        if (submitEventTeamId == null) {
            submitEventTeamId = resolveMyEventTeamId(match, player.getId(), memberId);
        }
        SubmitContext ctx = requireSubmitContext(match, submitEventTeamId, player.getId(), memberId);

        V4MatchSubmissionDetailResp resp = new V4MatchSubmissionDetailResp();
        resp.setMatchId(match.getId());
        resp.setMatchNo(match.getMatchNo());
        resp.setRoundNo(match.getRoundNo());
        resp.setMatchTime(match.getMatchTime());
        resp.setPlatformCode(match.getPlatformCode());
        resp.setHomeEventTeamId(match.getHomeEventTeamId());
        resp.setAwayEventTeamId(match.getAwayEventTeamId());
        resp.setSubmitEventTeamId(submitEventTeamId);
        fillTeamNames(resp, match);

        BasketMatchSubmission latest = findLatestSubmission(matchId, submitEventTeamId);
        if (latest != null) {
            resp.setSubmissionId(latest.getId());
            resp.setVersionNo(latest.getVersionNo());
            resp.setSubmissionStatus(latest.getSubmissionStatus());
            resp.setHomeScore(latest.getHomeScore());
            resp.setAwayScore(latest.getAwayScore());
            resp.setSubmitRemark(latest.getSubmitRemark());
            resp.setSubmitTime(latest.getSubmitTime());
            resp.setAuditUserName(latest.getAuditUserName());
            resp.setAuditTime(latest.getAuditTime());
            resp.setAuditRemark(latest.getAuditRemark());
            resp.setRejectReason(latest.getRejectReason());
            fillSubmissionPayload(resp, latest.getId());
        } else {
            resp.setVersionNo(1);
            resp.setSubmissionStatus(SubmissionStatusEnum.DRAFT.getCode());
        }
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(V4MatchSubmissionSaveReq req) {
        validateSaveReq(req);
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        BasketV4Match match = requirePublishedMatch(req.getMatchId());
        SubmitContext ctx = requireSubmitContext(match, req.getSubmitEventTeamId(), player.getId(), memberId);

        BasketMatchSubmission submission = findEditableSubmission(req.getMatchId(), req.getSubmitEventTeamId());
        Date now = new Date();
        if (submission == null) {
            submission = createNewSubmission(match, ctx, memberId, now);
        } else if (SubmissionStatusEnum.APPROVED.getCode().equals(submission.getSubmissionStatus())) {
            throw new ServiceException("已通过版本不可编辑");
        } else if (SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(submission.getSubmissionStatus())) {
            throw new ServiceException("审核中不可覆盖，请等待审核结果");
        }
        submission.setHomeScore(req.getHomeScore());
        submission.setAwayScore(req.getAwayScore());
        submission.setSubmitRemark(req.getSubmitRemark());
        submission.setSubmissionStatus(SubmissionStatusEnum.DRAFT.getCode());
        submission.setUpdateTime(now);
        submissionDao.updateById(submission);
        replaceSubmissionPayload(submission, req, now);
        return submission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(V4MatchSubmissionIdReq req) {
        if (req == null || req.getSubmissionId() == null) {
            throw new ServiceException("submissionId 不能为空");
        }
        Long memberId = UserContext.getRequiredMemberId();
        BasketMatchSubmission submission = submissionDao.selectById(req.getSubmissionId());
        if (submission == null || !NOT_DELETED.equals(submission.getIsDelete())) {
            throw new ServiceException("提交不存在");
        }
        if (!memberId.equals(submission.getSubmitMemberId())) {
            throw new ServiceException("无权提交该记录");
        }
        if (!SubmissionStatusEnum.DRAFT.getCode().equals(submission.getSubmissionStatus())
                && !SubmissionStatusEnum.REJECTED.getCode().equals(submission.getSubmissionStatus())) {
            throw new ServiceException("当前状态不可提交审核");
        }
        if (submission.getHomeScore() == null || submission.getAwayScore() == null) {
            throw new ServiceException("请先填写总比分");
        }
        Date now = new Date();
        submission.setSubmissionStatus(SubmissionStatusEnum.PENDING_REVIEW.getCode());
        submission.setSubmitTime(now);
        submission.setUpdateTime(now);
        submissionDao.updateById(submission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long resubmit(V4MatchSubmissionResubmitReq req) {
        if (req == null || req.getOldSubmissionId() == null) {
            throw new ServiceException("oldSubmissionId 不能为空");
        }
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        BasketMatchSubmission old = submissionDao.selectById(req.getOldSubmissionId());
        if (old == null || !NOT_DELETED.equals(old.getIsDelete())) {
            throw new ServiceException("原提交不存在");
        }
        if (!SubmissionStatusEnum.REJECTED.getCode().equals(old.getSubmissionStatus())) {
            throw new ServiceException("仅驳回状态可重提");
        }
        if (!memberId.equals(old.getSubmitMemberId())) {
            throw new ServiceException("无权重提该记录");
        }
        BasketV4Match match = requirePublishedMatch(old.getMatchId());
        SubmitContext ctx = requireSubmitContext(match, old.getSubmitEventTeamId(), player.getId(), memberId);
        Date now = new Date();
        int newVersion = (old.getVersionNo() != null ? old.getVersionNo() : 0) + 1;

        BasketMatchSubmission submission = new BasketMatchSubmission();
        submission.setMatchId(match.getId());
        submission.setSeasonId(match.getSeasonId());
        submission.setDivisionId(match.getDivisionId());
        submission.setSubmitEventTeamId(old.getSubmitEventTeamId());
        submission.setOpponentEventTeamId(old.getOpponentEventTeamId());
        submission.setSubmitEventPlayerId(ctx.eventPlayerId);
        submission.setSubmitMemberId(memberId);
        submission.setSubmitterRole(ctx.submitterRole);
        submission.setSubmissionStatus(SubmissionStatusEnum.DRAFT.getCode());
        submission.setVersionNo(newVersion);
        submission.setHomeScore(req.getHomeScore());
        submission.setAwayScore(req.getAwayScore());
        submission.setSubmitRemark(req.getSubmitRemark());
        submission.setCreateTime(now);
        submission.setUpdateTime(now);
        submission.setIsDelete(NOT_DELETED);
        submissionDao.insert(submission);

        V4MatchSubmissionSaveReq saveReq = new V4MatchSubmissionSaveReq();
        saveReq.setTeamStat(req.getTeamStat());
        saveReq.setPlayerStats(req.getPlayerStats());
        saveReq.setMaterialFileIds(req.getMaterialFileIds());
        saveReq.setSubmitEventTeamId(old.getSubmitEventTeamId());
        replaceSubmissionPayload(submission, saveReq, now);
        return submission.getId();
    }

    @Override
    public List<V4MySubmissionListResp> listMySubmissions() {
        return playerMatchSubmissionMapper.selectMySubmissions(UserContext.getRequiredMemberId());
    }

    private void enrichCanSubmit(V4MyMatchResp row, Long playerId) {
        row.setCanSubmit(false);
        if (row.getMyEventTeamId() == null) {
            row.setCannotSubmitReason("不属于本场比赛球队");
            return;
        }
        if (!MatchPublishStatusEnum.PUBLISHED.getCode().equals(row.getPublishStatus())) {
            row.setCannotSubmitReason("比赛未发布");
            return;
        }
        if (MatchStatusEnum.CANCELLED.getCode().equals(row.getMatchStatus())) {
            row.setCannotSubmitReason("比赛已取消");
            return;
        }
        if (SubmissionStatusEnum.PENDING_REVIEW.getCode().equals(row.getMySubmissionStatus())) {
            row.setCannotSubmitReason("本方审核中");
            return;
        }
        if (SubmissionStatusEnum.APPROVED.getCode().equals(row.getMySubmissionStatus())) {
            row.setCannotSubmitReason("本方已通过");
            return;
        }
        row.setCanSubmit(true);
        row.setCannotSubmitReason(null);
    }

    private BasketMatchSubmission findLatestSubmission(Long matchId, Long submitEventTeamId) {
        return submissionDao.selectOne(new LambdaQueryWrapper<BasketMatchSubmission>()
                .eq(BasketMatchSubmission::getMatchId, matchId)
                .eq(BasketMatchSubmission::getSubmitEventTeamId, submitEventTeamId)
                .eq(BasketMatchSubmission::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchSubmission::getVersionNo)
                .last("limit 1"));
    }

    private BasketMatchSubmission findEditableSubmission(Long matchId, Long submitEventTeamId) {
        BasketMatchSubmission latest = findLatestSubmission(matchId, submitEventTeamId);
        if (latest == null) {
            return null;
        }
        if (SubmissionStatusEnum.SUPERSEDED.getCode().equals(latest.getSubmissionStatus())
                || SubmissionStatusEnum.CANCELLED.getCode().equals(latest.getSubmissionStatus())) {
            return null;
        }
        return latest;
    }

    private BasketMatchSubmission createNewSubmission(BasketV4Match match, SubmitContext ctx, Long memberId, Date now) {
        Integer maxVer = submissionDao.selectList(new LambdaQueryWrapper<BasketMatchSubmission>()
                        .eq(BasketMatchSubmission::getMatchId, match.getId())
                        .eq(BasketMatchSubmission::getSubmitEventTeamId, ctx.eventTeamId)
                        .eq(BasketMatchSubmission::getIsDelete, NOT_DELETED))
                .stream().map(BasketMatchSubmission::getVersionNo).filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
        BasketMatchSubmission submission = new BasketMatchSubmission();
        submission.setMatchId(match.getId());
        submission.setSeasonId(match.getSeasonId());
        submission.setDivisionId(match.getDivisionId());
        submission.setSubmitEventTeamId(ctx.eventTeamId);
        submission.setOpponentEventTeamId(ctx.opponentEventTeamId);
        submission.setSubmitEventPlayerId(ctx.eventPlayerId);
        submission.setSubmitMemberId(memberId);
        submission.setSubmitterRole(ctx.submitterRole);
        submission.setSubmissionStatus(SubmissionStatusEnum.DRAFT.getCode());
        submission.setVersionNo(maxVer + 1);
        submission.setCreateTime(now);
        submission.setUpdateTime(now);
        submission.setIsDelete(NOT_DELETED);
        submissionDao.insert(submission);
        return submission;
    }

    private void replaceSubmissionPayload(BasketMatchSubmission submission, V4MatchSubmissionSaveReq req, Date now) {
        submissionTeamStatDao.delete(new LambdaQueryWrapper<BasketMatchSubmissionTeamStat>()
                .eq(BasketMatchSubmissionTeamStat::getSubmissionId, submission.getId()));
        submissionPlayerStatDao.delete(new LambdaQueryWrapper<BasketMatchSubmissionPlayerStat>()
                .eq(BasketMatchSubmissionPlayerStat::getSubmissionId, submission.getId()));
        submissionMaterialDao.delete(new LambdaQueryWrapper<BasketMatchSubmissionMaterial>()
                .eq(BasketMatchSubmissionMaterial::getSubmissionId, submission.getId()));

        if (req.getTeamStat() != null) {
            BasketMatchSubmissionTeamStat ts = new BasketMatchSubmissionTeamStat();
            BeanUtils.copyProperties(req.getTeamStat(), ts);
            ts.setSubmissionId(submission.getId());
            ts.setMatchId(submission.getMatchId());
            ts.setDivisionId(submission.getDivisionId());
            ts.setEventTeamId(submission.getSubmitEventTeamId());
            ts.setCreateTime(now);
            ts.setUpdateTime(now);
            ts.setIsDelete(NOT_DELETED);
            submissionTeamStatDao.insert(ts);
        }
        if (!CollectionUtils.isEmpty(req.getPlayerStats())) {
            for (V4MatchSubmissionPlayerStatReq psReq : req.getPlayerStats()) {
                validateEventPlayer(psReq.getEventPlayerId(), submission.getSubmitEventTeamId());
                BasketMatchSubmissionPlayerStat ps = new BasketMatchSubmissionPlayerStat();
                BeanUtils.copyProperties(psReq, ps);
                ps.setSubmissionId(submission.getId());
                ps.setMatchId(submission.getMatchId());
                ps.setDivisionId(submission.getDivisionId());
                ps.setEventTeamId(submission.getSubmitEventTeamId());
                ps.setCreateTime(now);
                ps.setUpdateTime(now);
                ps.setIsDelete(NOT_DELETED);
                submissionPlayerStatDao.insert(ps);
            }
        }
        if (!CollectionUtils.isEmpty(req.getMaterialFileIds())) {
            for (Long fileId : req.getMaterialFileIds()) {
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
    }

    private void fillSubmissionPayload(V4MatchSubmissionDetailResp resp, Long submissionId) {
        BasketMatchSubmissionTeamStat ts = submissionTeamStatDao.selectOne(new LambdaQueryWrapper<BasketMatchSubmissionTeamStat>()
                .eq(BasketMatchSubmissionTeamStat::getSubmissionId, submissionId)
                .eq(BasketMatchSubmissionTeamStat::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (ts != null) {
            V4MatchSubmissionTeamStatResp tsResp = new V4MatchSubmissionTeamStatResp();
            BeanUtils.copyProperties(ts, tsResp);
            resp.setTeamStat(tsResp);
        }
        List<V4MatchSubmissionPlayerStatResp> playerStats = new ArrayList<>();
        for (BasketMatchSubmissionPlayerStat ps : submissionPlayerStatDao.selectList(new LambdaQueryWrapper<BasketMatchSubmissionPlayerStat>()
                .eq(BasketMatchSubmissionPlayerStat::getSubmissionId, submissionId)
                .eq(BasketMatchSubmissionPlayerStat::getIsDelete, NOT_DELETED))) {
            V4MatchSubmissionPlayerStatResp psResp = new V4MatchSubmissionPlayerStatResp();
            BeanUtils.copyProperties(ps, psResp);
            BasketEventPlayer ep = eventPlayerDao.selectById(ps.getEventPlayerId());
            if (ep != null) {
                psResp.setPlayerName(ep.getPlayerNameSnapshot());
            }
            playerStats.add(psResp);
        }
        resp.setPlayerStats(playerStats);
        List<Long> fileIds = submissionMaterialDao.selectList(new LambdaQueryWrapper<BasketMatchSubmissionMaterial>()
                        .eq(BasketMatchSubmissionMaterial::getSubmissionId, submissionId)
                        .eq(BasketMatchSubmissionMaterial::getIsDelete, NOT_DELETED))
                .stream().map(BasketMatchSubmissionMaterial::getFileId).toList();
        resp.setMaterialFileIds(fileIds);
    }

    private void fillTeamNames(V4MatchSubmissionDetailResp resp, BasketV4Match match) {
        BasketEventTeam ht = eventTeamDao.selectById(match.getHomeEventTeamId());
        if (ht != null) {
            resp.setHomeTeamName(ht.getTeamNameSnapshot());
        }
        BasketEventTeam at = eventTeamDao.selectById(match.getAwayEventTeamId());
        if (at != null) {
            resp.setAwayTeamName(at.getTeamNameSnapshot());
        }
        BasketEventTeam st = eventTeamDao.selectById(resp.getSubmitEventTeamId());
        if (st != null) {
            resp.setSubmitTeamName(st.getTeamNameSnapshot());
        }
    }

    private Long resolveMyEventTeamId(BasketV4Match match, Long playerId, Long memberId) {
        if (belongsToEventTeam(match.getHomeEventTeamId(), playerId, memberId)) {
            return match.getHomeEventTeamId();
        }
        if (belongsToEventTeam(match.getAwayEventTeamId(), playerId, memberId)) {
            return match.getAwayEventTeamId();
        }
        if (isEventTeamCaptain(match.getHomeEventTeamId(), playerId)) {
            return match.getHomeEventTeamId();
        }
        if (isEventTeamCaptain(match.getAwayEventTeamId(), playerId)) {
            return match.getAwayEventTeamId();
        }
        throw new ServiceException("无权查看该比赛提交");
    }

    private SubmitContext requireSubmitContext(BasketV4Match match, Long submitEventTeamId, Long playerId, Long memberId) {
        if (!submitEventTeamId.equals(match.getHomeEventTeamId()) && !submitEventTeamId.equals(match.getAwayEventTeamId())) {
            throw new ServiceException("submitEventTeamId 必须是主队或客队");
        }
        Long opponentId = submitEventTeamId.equals(match.getHomeEventTeamId()) ? match.getAwayEventTeamId() : match.getHomeEventTeamId();
        BasketEventPlayer eventPlayer = findEventPlayer(submitEventTeamId, playerId, memberId);
        String role = SubmitterRoleEnum.PLAYER.getCode();
        Long eventPlayerId = eventPlayer != null ? eventPlayer.getId() : null;
        if (eventPlayer == null) {
            if (!isAuthorizedCaptain(match.getDivisionId(), submitEventTeamId, playerId)) {
                throw new ServiceException("无权提交该队比赛数据");
            }
            role = SubmitterRoleEnum.CAPTAIN.getCode();
            eventPlayerId = findCaptainEventPlayerId(submitEventTeamId, playerId);
        } else if (Integer.valueOf(1).equals(eventPlayer.getIsCaptain())) {
            role = SubmitterRoleEnum.CAPTAIN.getCode();
        }
        SubmitContext ctx = new SubmitContext();
        ctx.eventTeamId = submitEventTeamId;
        ctx.opponentEventTeamId = opponentId;
        ctx.eventPlayerId = eventPlayerId;
        ctx.submitterRole = role;
        return ctx;
    }

    private boolean belongsToEventTeam(Long eventTeamId, Long playerId, Long memberId) {
        return findEventPlayer(eventTeamId, playerId, memberId) != null;
    }

    private BasketEventPlayer findEventPlayer(Long eventTeamId, Long playerId, Long memberId) {
        return eventPlayerDao.selectOne(new LambdaQueryWrapper<BasketEventPlayer>()
                .eq(BasketEventPlayer::getEventTeamId, eventTeamId)
                .eq(BasketEventPlayer::getIsDelete, NOT_DELETED)
                .and(w -> w.eq(BasketEventPlayer::getSourcePlayerId, playerId).or().eq(BasketEventPlayer::getMemberId, memberId))
                .last("limit 1"));
    }

    private boolean isEventTeamCaptain(Long eventTeamId, Long playerId) {
        BasketEventTeam team = eventTeamDao.selectById(eventTeamId);
        return team != null && playerId.equals(team.getCaptainPlayerId());
    }

    private boolean isAuthorizedCaptain(Long divisionId, Long eventTeamId, Long playerId) {
        if (isEventTeamCaptain(eventTeamId, playerId)) {
            return true;
        }
        BasketEventTeam eventTeam = eventTeamDao.selectById(eventTeamId);
        if (eventTeam == null || eventTeam.getSourceTeamId() == null) {
            return false;
        }
        BasketCaptainGrant grant = captainGrantDao.selectOne(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getDivisionId, divisionId)
                .eq(BasketCaptainGrant::getTeamId, eventTeam.getSourceTeamId())
                .eq(BasketCaptainGrant::getPlayerId, playerId)
                .eq(BasketCaptainGrant::getGrantStatus, CaptainGrantStatusEnum.ACTIVE.getCode())
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNotNull(BasketCaptainGrant::getActiveUniqueKey)
                .last("limit 1"));
        return grant != null;
    }

    private Long findCaptainEventPlayerId(Long eventTeamId, Long playerId) {
        BasketEventPlayer ep = eventPlayerDao.selectOne(new LambdaQueryWrapper<BasketEventPlayer>()
                .eq(BasketEventPlayer::getEventTeamId, eventTeamId)
                .eq(BasketEventPlayer::getSourcePlayerId, playerId)
                .eq(BasketEventPlayer::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        return ep != null ? ep.getId() : null;
    }

    private void validateEventPlayer(Long eventPlayerId, Long submitEventTeamId) {
        BasketEventPlayer ep = eventPlayerDao.selectById(eventPlayerId);
        if (ep == null || !NOT_DELETED.equals(ep.getIsDelete()) || !submitEventTeamId.equals(ep.getEventTeamId())) {
            throw new ServiceException("球员不属于提交方球队");
        }
    }

    private BasketV4Match requireMatch(Long matchId) {
        BasketV4Match match = matchDao.selectById(matchId);
        if (match == null || !NOT_DELETED.equals(match.getIsDelete())) {
            throw new ServiceException("比赛不存在");
        }
        return match;
    }

    private BasketV4Match requirePublishedMatch(Long matchId) {
        BasketV4Match match = requireMatch(matchId);
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

    private void validateSaveReq(V4MatchSubmissionSaveReq req) {
        if (req == null || req.getMatchId() == null || req.getSubmitEventTeamId() == null) {
            throw new ServiceException("matchId / submitEventTeamId 不能为空");
        }
    }

    private static class SubmitContext {
        private Long eventTeamId;
        private Long opponentEventTeamId;
        private Long eventPlayerId;
        private String submitterRole;
    }
}
