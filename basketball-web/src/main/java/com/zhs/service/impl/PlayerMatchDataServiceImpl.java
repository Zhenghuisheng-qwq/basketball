package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketMatchDao;
import com.zhs.dao.BasketMatchDataAuditDao;
import com.zhs.dao.BasketMatchDataSubmissionDao;
import com.zhs.dao.BasketMatchDataSubmissionMaterialDao;
import com.zhs.dao.BasketMatchDataSubmissionPeriodDao;
import com.zhs.dao.BasketMatchDataSubmissionPlayerStatDao;
import com.zhs.dao.BasketMatchDataSubmissionTeamStatDao;
import com.zhs.dao.BasketMatchRosterDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTournamentRegistrationDao;
import com.zhs.dao.BasketTournamentSeasonRosterDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.enums.MatchDataMaterialTypeEnum;
import com.zhs.enums.MatchDataSubmissionStatusEnum;
import com.zhs.enums.MatchPeriodTypeEnum;
import com.zhs.enums.RosterStatusEnum;
import com.zhs.enums.TournamentRegistrationStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMatch;
import com.zhs.model.BasketMatchDataAudit;
import com.zhs.model.BasketMatchDataSubmission;
import com.zhs.model.BasketMatchDataSubmissionMaterial;
import com.zhs.model.BasketMatchDataSubmissionPeriod;
import com.zhs.model.BasketMatchDataSubmissionPlayerStat;
import com.zhs.model.BasketMatchDataSubmissionTeamStat;
import com.zhs.model.BasketMatchRoster;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTournamentRegistration;
import com.zhs.model.BasketTournamentSeasonRoster;
import com.zhs.request.matchdata.PlayerMatchDataResubmitReq;
import com.zhs.request.matchdata.PlayerMatchDataSubmitReq;
import com.zhs.response.matchdata.PlayerMatchDataStatusResp;
import com.zhs.response.matchdata.PlayerMatchDataSubmissionDetailResp;
import com.zhs.response.matchdata.PlayerMatchDataSubmitResultResp;
import com.zhs.response.matchdata.PlayerMatchTaskResp;
import com.zhs.service.IAccountFileService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.IMPlayerMatchDataService;
import com.zhs.service.support.BasketPublicConstants;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlayerMatchDataServiceImpl implements IMPlayerMatchDataService {

    private static final Integer NOT_DELETED = 0;
    private static final String AUDIT_TYPE_RESULT = "RESULT";
    private static final String AUDIT_STATUS_PENDING = "PENDING";
    private static final String SUBMITTER_ROLE_CAPTAIN = "CAPTAIN";
    private static final String SIDE_HOME = "HOME";
    private static final String SIDE_AWAY = "AWAY";
    private static final int MATERIAL_STATUS_ACTIVE = 1;
    private static final int REQUIRED_REGULAR_PERIODS = 4;

    @Resource
    private BasketTournamentSeasonRosterDao seasonRosterDao;
    @Resource
    private BasketTournamentRegistrationDao registrationDao;
    @Resource
    private BasketMatchDao matchDao;
    @Resource
    private BasketMatchRosterDao matchRosterDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketMatchDataSubmissionDao submissionDao;
    @Resource
    private BasketMatchDataSubmissionPeriodDao submissionPeriodDao;
    @Resource
    private BasketMatchDataSubmissionTeamStatDao submissionTeamStatDao;
    @Resource
    private BasketMatchDataSubmissionPlayerStatDao submissionPlayerStatDao;
    @Resource
    private BasketMatchDataSubmissionMaterialDao submissionMaterialDao;
    @Resource
    private BasketMatchDataAuditDao auditDao;
    @Resource
    private IAccountFileService accountFileService;
    @Resource
    private ICaptainPermissionService captainPermissionService;

    @Override
    public IPage<PlayerMatchTaskResp> pageMyMatches(Long seasonId, Long stageId, Integer dataStatus,
                                                    Integer matchStatus, Integer pageNum, Integer pageSize) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);

        int current = pageNum == null || pageNum < 1 ? BasketPublicConstants.DEFAULT_PAGE : pageNum;
        int size = pageSize == null || pageSize < 1 ? BasketPublicConstants.DEFAULT_SIZE : Math.min(pageSize, BasketPublicConstants.MAX_SIZE);

        List<Long> myTeamIds = findActiveTeamIds(memberId, seasonId);
        if (myTeamIds.isEmpty()) {
            return emptyPage(current, size);
        }

        LambdaQueryWrapper<BasketMatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatch::getIsDelete, NOT_DELETED)
                .and(w -> w.in(BasketMatch::getHomeTeamId, myTeamIds).or().in(BasketMatch::getAwayTeamId, myTeamIds));
        if (seasonId != null) {
            wrapper.eq(BasketMatch::getSeasonId, seasonId);
        }
        if (stageId != null) {
            wrapper.eq(BasketMatch::getStageId, stageId);
        }
        if (dataStatus != null) {
            wrapper.eq(BasketMatch::getDataStatus, dataStatus);
        }
        if (matchStatus != null) {
            wrapper.eq(BasketMatch::getMatchStatus, matchStatus);
        }
        wrapper.orderByDesc(BasketMatch::getScheduledStartTime);

        Page<BasketMatch> page = matchDao.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> teamNameMap = loadTeamNames(page.getRecords());
        Map<Long, BasketMatchDataSubmission> latestSubmissionMap = loadLatestSubmissions(
                page.getRecords().stream().map(BasketMatch::getId).collect(Collectors.toList()));
        Map<Long, Long> captainTeamMap = loadCaptainTeamMap(memberId, page.getRecords());

        Page<PlayerMatchTaskResp> respPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<PlayerMatchTaskResp> records = new ArrayList<>();
        for (BasketMatch match : page.getRecords()) {
            records.add(toTaskResp(match, myTeamIds, teamNameMap, latestSubmissionMap.get(match.getId()), captainTeamMap));
        }
        respPage.setRecords(records);
        return respPage;
    }

    @Override
    public PlayerMatchDataSubmissionDetailResp getSubmitDetail(Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketMatch match = requireMatch(matchId);
        assertMatchFinished(match);
        assertMemberInSeason(memberId, match.getSeasonId());

        List<Long> captainTeamIds = findCaptainTeamIdsForMatch(memberId, match);
        if (captainTeamIds.isEmpty()) {
            throw new ServiceException("仅参赛球队队长可提交比赛数据", 403);
        }

        PlayerMatchDataSubmissionDetailResp resp = buildBaseDetailResp(match, captainTeamIds);
        resp.setPeriodScores(defaultRegularPeriods());
        resp.setTeamStats(defaultTeamStats(match));
        resp.setPlayerStats(buildPlayerStatTemplate(match.getId()));
        fillSubmitAvailability(resp, match);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerMatchDataSubmitResultResp submit(Long matchId, PlayerMatchDataSubmitReq req) {
        return doSubmit(matchId, req, false, null);
    }

    @Override
    public PlayerMatchDataStatusResp getStatus(Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketMatch match = requireMatch(matchId);
        assertMemberCanViewMatch(memberId, match);

        BasketMatchDataSubmission submission = findLatestSubmission(matchId);
        if (submission == null) {
            throw new ServiceException("该比赛暂无提交记录");
        }

        return toStatusResp(match, submission, memberId);
    }

    @Override
    public PlayerMatchDataSubmissionDetailResp getResubmitDetail(Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketMatch match = requireMatch(matchId);
        assertMemberCanViewMatch(memberId, match);

        BasketMatchDataSubmission submission = findLatestSubmission(matchId);
        if (submission == null || !Objects.equals(submission.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode())) {
            throw new ServiceException("仅驳回后可重新提交");
        }
        assertCaptainOfTeam(memberId, match.getSeasonId(), submission.getSubmitterTeamId());

        List<Long> captainTeamIds = Collections.singletonList(submission.getSubmitterTeamId());
        PlayerMatchDataSubmissionDetailResp resp = buildBaseDetailResp(match, captainTeamIds);
        fillSubmissionDetail(resp, submission);
        resp.setCanSubmit(true);
        resp.setCannotSubmitReason(null);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerMatchDataSubmitResultResp resubmit(Long matchId, PlayerMatchDataResubmitReq req) {
        BasketMatchDataSubmission latest = findLatestSubmission(matchId);
        if (latest == null || !Objects.equals(latest.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode())) {
            throw new ServiceException("仅驳回后可重新提交");
        }
        Long memberId = UserContext.getRequiredMemberId();
        assertCaptainOfTeam(memberId, latest.getSeasonId(), latest.getSubmitterTeamId());
        return doSubmit(matchId, req, true, latest);
    }

    private PlayerMatchDataSubmitResultResp doSubmit(Long matchId, PlayerMatchDataSubmitReq req, boolean resubmit,
                                                     BasketMatchDataSubmission previous) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile profile = captainPermissionService.requireCertifiedPlayer(memberId);

        BasketMatch match = matchDao.selectByIdForUpdate(matchId);
        if (match == null || !NOT_DELETED.equals(match.getIsDelete())) {
            throw new ServiceException("比赛不存在");
        }
        assertMatchFinished(match);

        if (resubmit) {
            if (previous == null || !Objects.equals(previous.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode())) {
                throw new ServiceException("仅驳回后可重新提交");
            }
        } else {
            assertNoActiveSubmission(matchId);
            Integer currentDataStatus = match.getDataStatus();
            if (currentDataStatus != null && currentDataStatus == BasketPublicConstants.DATA_CONFIRMED) {
                throw new ServiceException("比赛数据已确认，无法提交");
            }
        }

        List<Long> captainTeamIds = findCaptainTeamIdsForMatch(memberId, match);
        if (captainTeamIds.isEmpty()) {
            throw new ServiceException("仅参赛球队队长可提交比赛数据", 403);
        }
        Long submitterTeamId;
        if (resubmit) {
            submitterTeamId = previous.getSubmitterTeamId();
            if (!captainTeamIds.contains(submitterTeamId)) {
                throw new ServiceException("重新提交须由同一提交方球队队长操作");
            }
        } else {
            submitterTeamId = captainTeamIds.get(0);
        }
        assertCaptainOfTeam(memberId, match.getSeasonId(), submitterTeamId);

        ValidatedSubmissionPayload payload = validateAndBuildPayload(match, req);

        Date now = new Date();
        int versionNo = resubmit ? previous.getVersionNo() + 1 : 1;
        Long previousSubmissionId = resubmit ? previous.getId() : null;

        BasketMatchDataSubmission submission = new BasketMatchDataSubmission();
        submission.setSubmissionNo(generateSubmissionNo());
        submission.setMatchId(match.getId());
        submission.setSeasonId(match.getSeasonId());
        submission.setStageId(match.getStageId());
        submission.setRoundId(match.getRoundId());
        submission.setGroupId(match.getGroupId());
        submission.setPlatformCode(match.getPlatformCode());
        submission.setHomeTeamId(match.getHomeTeamId());
        submission.setAwayTeamId(match.getAwayTeamId());
        submission.setSubmitterTeamId(submitterTeamId);
        submission.setSubmitterMemberId(memberId);
        submission.setSubmitterPlayerId(profile.getId());
        submission.setSubmitterRole(SUBMITTER_ROLE_CAPTAIN);
        submission.setVersionNo(versionNo);
        submission.setPreviousSubmissionId(previousSubmissionId);
        submission.setHomeScore(payload.homeScore);
        submission.setAwayScore(payload.awayScore);
        submission.setWinnerTeamId(payload.winnerTeamId);
        submission.setSubmissionStatus(MatchDataSubmissionStatusEnum.PENDING.getCode());
        submission.setAuditStatus(AUDIT_STATUS_PENDING);
        submission.setSubmittedTime(now);
        submission.setRemark(trimToNull(req.getRemark()));
        submission.setCreateTime(now);
        submission.setUpdateTime(now);
        submission.setIsDelete(NOT_DELETED);

        try {
            submissionDao.insert(submission);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("该比赛已有提交中的数据，请勿重复提交");
        }

        insertChildren(submission, match, payload, now);
        insertAuditRecord(match, submission, memberId, now, resubmit ? "RESUBMIT" : "SUBMIT");

        if (resubmit) {
            BasketMatchDataSubmission superseded = new BasketMatchDataSubmission();
            superseded.setId(previous.getId());
            superseded.setSubmissionStatus(MatchDataSubmissionStatusEnum.SUPERSEDED.getCode());
            superseded.setUpdateTime(now);
            submissionDao.updateById(superseded);
        }

        if (matchDao.updateDataStatusIfAllowed(matchId, BasketPublicConstants.DATA_SUBMITTED, NOT_DELETED) != 1
                && !Objects.equals(match.getDataStatus(), BasketPublicConstants.DATA_SUBMITTED)) {
            throw new ServiceException("比赛数据状态不可提交");
        }

        PlayerMatchDataSubmitResultResp resp = new PlayerMatchDataSubmitResultResp();
        resp.setSubmissionId(String.valueOf(submission.getId()));
        resp.setSubmissionNo(submission.getSubmissionNo());
        resp.setVersionNo(submission.getVersionNo());
        return resp;
    }

    private void insertChildren(BasketMatchDataSubmission submission, BasketMatch match,
                                ValidatedSubmissionPayload payload, Date now) {
        int sort = 0;
        for (BasketMatchDataSubmissionPeriod period : payload.periods) {
            period.setSubmissionId(submission.getId());
            period.setMatchId(match.getId());
            period.setSortNo(sort++);
            period.setCreateTime(now);
            period.setUpdateTime(now);
            period.setIsDelete(NOT_DELETED);
            submissionPeriodDao.insert(period);
        }
        sort = 0;
        for (BasketMatchDataSubmissionTeamStat stat : payload.teamStats) {
            stat.setSubmissionId(submission.getId());
            stat.setMatchId(match.getId());
            stat.setSeasonId(match.getSeasonId());
            stat.setStageId(match.getStageId());
            stat.setSortNo(sort++);
            stat.setCreateTime(now);
            stat.setUpdateTime(now);
            stat.setIsDelete(NOT_DELETED);
            submissionTeamStatDao.insert(stat);
        }
        sort = 0;
        for (BasketMatchDataSubmissionPlayerStat stat : payload.playerStats) {
            stat.setSubmissionId(submission.getId());
            stat.setMatchId(match.getId());
            stat.setSeasonId(match.getSeasonId());
            stat.setStageId(match.getStageId());
            stat.setSortNo(sort++);
            stat.setCreateTime(now);
            stat.setUpdateTime(now);
            stat.setIsDelete(NOT_DELETED);
            submissionPlayerStatDao.insert(stat);
        }
        sort = 0;
        for (BasketMatchDataSubmissionMaterial material : payload.materials) {
            material.setSubmissionId(submission.getId());
            material.setMatchId(match.getId());
            material.setSortNo(sort++);
            material.setMaterialStatus(MATERIAL_STATUS_ACTIVE);
            material.setCreateTime(now);
            material.setUpdateTime(now);
            material.setIsDelete(NOT_DELETED);
            submissionMaterialDao.insert(material);
        }
    }

    private void insertAuditRecord(BasketMatch match, BasketMatchDataSubmission submission,
                                   Long memberId, Date now, String auditAction) {
        BasketMatchDataAudit audit = new BasketMatchDataAudit();
        audit.setMatchId(match.getId());
        audit.setSubmissionId(submission.getId());
        audit.setDataVersion(submission.getVersionNo());
        audit.setAuditType(AUDIT_TYPE_RESULT);
        audit.setAuditAction(auditAction);
        audit.setBeforeStatus(String.valueOf(match.getDataStatus()));
        audit.setAfterStatus(AUDIT_STATUS_PENDING);
        audit.setSubmitterId(memberId);
        audit.setSubmitTime(now);
        audit.setAuditStatus(AUDIT_STATUS_PENDING);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        auditDao.insert(audit);
    }

    private ValidatedSubmissionPayload validateAndBuildPayload(BasketMatch match, PlayerMatchDataSubmitReq req) {
        if (CollectionUtils.isEmpty(req.getPeriodScores())) {
            throw new ServiceException("分节比分不能为空");
        }
        if (CollectionUtils.isEmpty(req.getTeamStats()) || req.getTeamStats().size() != 2) {
            throw new ServiceException("须录入双方球队技术统计");
        }
        if (CollectionUtils.isEmpty(req.getPlayerStats())) {
            throw new ServiceException("球员技术统计不能为空");
        }
        if (CollectionUtils.isEmpty(req.getMaterialFileIds())) {
            throw new ServiceException("请上传比赛结果截图");
        }

        int homeScore = nvl(req.getHomeScore());
        int awayScore = nvl(req.getAwayScore());
        if (homeScore < 0 || awayScore < 0) {
            throw new ServiceException("比分不能为负数");
        }

        List<BasketMatchDataSubmissionPeriod> periods = new ArrayList<>();
        int homePeriodTotal = 0;
        int awayPeriodTotal = 0;
        int otCount = 0;
        Set<Integer> regularPeriodNos = new HashSet<>();
        for (PlayerMatchDataSubmitReq.PeriodScoreItem item : req.getPeriodScores()) {
            if (item.getPeriodNo() == null || !StringUtils.hasText(item.getPeriodType())) {
                throw new ServiceException("节次信息不完整");
            }
            MatchPeriodTypeEnum periodType = MatchPeriodTypeEnum.fromCode(item.getPeriodType());
            int periodHome = nvl(item.getHomeScore());
            int periodAway = nvl(item.getAwayScore());
            if (periodHome < 0 || periodAway < 0) {
                throw new ServiceException("节次比分不能为负数");
            }
            homePeriodTotal += periodHome;
            awayPeriodTotal += periodAway;
            if (periodType == MatchPeriodTypeEnum.REGULAR) {
                regularPeriodNos.add(item.getPeriodNo());
            } else {
                otCount++;
            }
            BasketMatchDataSubmissionPeriod period = new BasketMatchDataSubmissionPeriod();
            period.setPeriodNo(item.getPeriodNo());
            period.setPeriodType(periodType.getCode());
            period.setHomeScore(periodHome);
            period.setAwayScore(periodAway);
            periods.add(period);
        }
        for (int i = 1; i <= REQUIRED_REGULAR_PERIODS; i++) {
            if (!regularPeriodNos.contains(i)) {
                throw new ServiceException("须录入四节常规比分");
            }
        }
        if (homePeriodTotal != homeScore || awayPeriodTotal != awayScore) {
            throw new ServiceException("分节比分之和须与最终比分一致");
        }
        if (homeScore == awayScore && otCount == 0) {
            throw new ServiceException("常规时间平局须通过加时赛分出胜负");
        }

        Long winnerTeamId = null;
        if (homeScore > awayScore) {
            winnerTeamId = match.getHomeTeamId();
        } else if (awayScore > homeScore) {
            winnerTeamId = match.getAwayTeamId();
        }

        Map<Long, BasketMatchDataSubmissionTeamStat> teamStatMap = new HashMap<>();
        List<BasketMatchDataSubmissionTeamStat> teamStats = new ArrayList<>();
        for (PlayerMatchDataSubmitReq.TeamStatItem item : req.getTeamStats()) {
            Long teamId = parseLongId(item.getTeamId(), "球队");
            if (!teamId.equals(match.getHomeTeamId()) && !teamId.equals(match.getAwayTeamId())) {
                throw new ServiceException("球队须为参赛主客队");
            }
            validateTeamStatItem(item);
            Long opponentId = teamId.equals(match.getHomeTeamId()) ? match.getAwayTeamId() : match.getHomeTeamId();
            BasketMatchDataSubmissionTeamStat stat = buildTeamStat(match, item, teamId, opponentId);
            teamStatMap.put(teamId, stat);
            teamStats.add(stat);
        }
        BasketMatchDataSubmissionTeamStat homeStat = teamStatMap.get(match.getHomeTeamId());
        BasketMatchDataSubmissionTeamStat awayStat = teamStatMap.get(match.getAwayTeamId());
        if (homeStat == null || awayStat == null) {
            throw new ServiceException("缺少主客队技术统计");
        }
        if (!Objects.equals(homeStat.getPoints(), homeScore) || !Objects.equals(awayStat.getPoints(), awayScore)) {
            throw new ServiceException("球队得分须与比赛比分一致");
        }

        Map<Long, Set<Long>> rosterPlayerMap = loadRosterPlayerMap(match.getId());
        List<BasketMatchDataSubmissionPlayerStat> playerStats = new ArrayList<>();
        Map<Long, int[]> teamAgg = new HashMap<>();
        teamAgg.put(match.getHomeTeamId(), new int[7]);
        teamAgg.put(match.getAwayTeamId(), new int[7]);
        int totalPlayerPoints = 0;
        for (PlayerMatchDataSubmitReq.PlayerStatItem item : req.getPlayerStats()) {
            Long teamId = parseLongId(item.getTeamId(), "球队");
            Long playerId = parseLongId(item.getPlayerId(), "球员");
            if (!teamId.equals(match.getHomeTeamId()) && !teamId.equals(match.getAwayTeamId())) {
                throw new ServiceException("球员所属球队无效");
            }
            Set<Long> rosterPlayers = rosterPlayerMap.get(teamId);
            if (CollectionUtils.isEmpty(rosterPlayers) || !rosterPlayers.contains(playerId)) {
                throw new ServiceException("球员须来自本场比赛出场名单");
            }
            validatePlayerStatItem(item);
            BasketMatchDataSubmissionPlayerStat stat = buildPlayerStat(match, item, teamId, playerId);
            playerStats.add(stat);
            totalPlayerPoints += nvl(item.getPoints());
            int[] agg = teamAgg.get(teamId);
            agg[0] += nvl(item.getPoints());
            agg[1] += nvl(item.getRebounds());
            agg[2] += nvl(item.getAssists());
            agg[3] += nvl(item.getSteals());
            agg[4] += nvl(item.getBlocks());
            agg[5] += nvl(item.getTurnovers());
            agg[6] += nvl(item.getFouls());
        }
        if (totalPlayerPoints != homeScore + awayScore) {
            throw new ServiceException("球员得分合计须等于比赛总得分");
        }

        Long memberId = UserContext.getRequiredMemberId();
        List<BasketMatchDataSubmissionMaterial> materials = new ArrayList<>();
        boolean hasResultScreenshot = false;
        for (String fileIdText : req.getMaterialFileIds()) {
            Long fileId = parseLongId(fileIdText, "材料文件");
            BasketFileAsset asset = accountFileService.requireOwnedFile(fileId, memberId);
            if (!FileUsageTypeEnum.MATCH_DATA_SCREENSHOT.getCode().equalsIgnoreCase(asset.getUsageType())) {
                throw new ServiceException("材料文件用途不匹配，请使用比赛数据截图");
            }
            hasResultScreenshot = true;
            BasketMatchDataSubmissionMaterial material = new BasketMatchDataSubmissionMaterial();
            material.setFileId(fileId);
            material.setMaterialType(MatchDataMaterialTypeEnum.RESULT_SCREENSHOT.getCode());
            material.setMaterialName(asset.getOriginalName());
            materials.add(material);
        }
        if (!hasResultScreenshot) {
            throw new ServiceException("请至少上传一张比赛结果截图");
        }

        ValidatedSubmissionPayload payload = new ValidatedSubmissionPayload();
        payload.homeScore = homeScore;
        payload.awayScore = awayScore;
        payload.winnerTeamId = winnerTeamId;
        payload.periods = periods;
        payload.teamStats = teamStats;
        payload.playerStats = playerStats;
        payload.materials = materials;
        return payload;
    }

    private BasketMatchDataSubmissionTeamStat buildTeamStat(BasketMatch match, PlayerMatchDataSubmitReq.TeamStatItem item,
                                                          Long teamId, Long opponentId) {
        BasketMatchDataSubmissionTeamStat stat = new BasketMatchDataSubmissionTeamStat();
        stat.setTeamId(teamId);
        stat.setOpponentTeamId(opponentId);
        stat.setSideType(teamId.equals(match.getHomeTeamId()) ? SIDE_HOME : SIDE_AWAY);
        stat.setPoints(nvl(item.getPoints()));
        stat.setRebounds(nvl(item.getRebounds()));
        stat.setOffensiveRebounds(nvl(item.getOffensiveRebounds()));
        stat.setDefensiveRebounds(nvl(item.getDefensiveRebounds()));
        stat.setAssists(nvl(item.getAssists()));
        stat.setSteals(nvl(item.getSteals()));
        stat.setBlocks(nvl(item.getBlocks()));
        stat.setTurnovers(nvl(item.getTurnovers()));
        stat.setFouls(nvl(item.getFouls()));
        stat.setFieldGoalsMade(nvl(item.getFieldGoalsMade()));
        stat.setFieldGoalsAttempted(nvl(item.getFieldGoalsAttempted()));
        stat.setFieldGoalPct(computePct(item.getFieldGoalsMade(), item.getFieldGoalsAttempted()));
        stat.setThreePointsMade(nvl(item.getThreePointsMade()));
        stat.setThreePointsAttempted(nvl(item.getThreePointsAttempted()));
        stat.setThreePointPct(computePct(item.getThreePointsMade(), item.getThreePointsAttempted()));
        stat.setFreeThrowsMade(nvl(item.getFreeThrowsMade()));
        stat.setFreeThrowsAttempted(nvl(item.getFreeThrowsAttempted()));
        stat.setFreeThrowPct(computePct(item.getFreeThrowsMade(), item.getFreeThrowsAttempted()));
        stat.setPossessions(item.getPossessions() != null ? item.getPossessions() : BigDecimal.ZERO);
        stat.setEfficiency(item.getEfficiency() != null ? item.getEfficiency() : BigDecimal.ZERO);
        return stat;
    }

    private BasketMatchDataSubmissionPlayerStat buildPlayerStat(BasketMatch match, PlayerMatchDataSubmitReq.PlayerStatItem item,
                                                                Long teamId, Long playerId) {
        BasketMatchDataSubmissionPlayerStat stat = new BasketMatchDataSubmissionPlayerStat();
        stat.setTeamId(teamId);
        stat.setPlayerId(playerId);
        if (StringUtils.hasText(item.getRosterId())) {
            stat.setRosterId(parseLongId(item.getRosterId(), "出场名单"));
        }
        stat.setStartedFlag(item.getStartedFlag() != null ? item.getStartedFlag() : 0);
        stat.setMinutesSeconds(nvl(item.getMinutesSeconds()));
        stat.setPoints(nvl(item.getPoints()));
        stat.setRebounds(nvl(item.getRebounds()));
        stat.setOffensiveRebounds(nvl(item.getOffensiveRebounds()));
        stat.setDefensiveRebounds(nvl(item.getDefensiveRebounds()));
        stat.setAssists(nvl(item.getAssists()));
        stat.setSteals(nvl(item.getSteals()));
        stat.setBlocks(nvl(item.getBlocks()));
        stat.setTurnovers(nvl(item.getTurnovers()));
        stat.setFouls(nvl(item.getFouls()));
        stat.setFieldGoalsMade(nvl(item.getFieldGoalsMade()));
        stat.setFieldGoalsAttempted(nvl(item.getFieldGoalsAttempted()));
        stat.setFieldGoalPct(computePct(item.getFieldGoalsMade(), item.getFieldGoalsAttempted()));
        stat.setThreePointsMade(nvl(item.getThreePointsMade()));
        stat.setThreePointsAttempted(nvl(item.getThreePointsAttempted()));
        stat.setThreePointPct(computePct(item.getThreePointsMade(), item.getThreePointsAttempted()));
        stat.setFreeThrowsMade(nvl(item.getFreeThrowsMade()));
        stat.setFreeThrowsAttempted(nvl(item.getFreeThrowsAttempted()));
        stat.setFreeThrowPct(computePct(item.getFreeThrowsMade(), item.getFreeThrowsAttempted()));
        stat.setPlusMinus(nvl(item.getPlusMinus()));
        stat.setEfficiency(item.getEfficiency() != null ? item.getEfficiency() : BigDecimal.ZERO);
        return stat;
    }

    private void validateTeamStatItem(PlayerMatchDataSubmitReq.TeamStatItem item) {
        validateShooting(item.getFieldGoalsMade(), item.getFieldGoalsAttempted(), "投篮");
        validateShooting(item.getThreePointsMade(), item.getThreePointsAttempted(), "三分");
        validateShooting(item.getFreeThrowsMade(), item.getFreeThrowsAttempted(), "罚球");
        validateThreeWithinFg(item.getFieldGoalsMade(), item.getThreePointsMade(), "三分");
        validateTheoreticalPoints(item.getPoints(), item.getFieldGoalsMade(), item.getThreePointsMade(),
                item.getFreeThrowsMade(), "球队");
    }

    private void validatePlayerStatItem(PlayerMatchDataSubmitReq.PlayerStatItem item) {
        validateShooting(item.getFieldGoalsMade(), item.getFieldGoalsAttempted(), "投篮");
        validateShooting(item.getThreePointsMade(), item.getThreePointsAttempted(), "三分");
        validateShooting(item.getFreeThrowsMade(), item.getFreeThrowsAttempted(), "罚球");
        validateThreeWithinFg(item.getFieldGoalsMade(), item.getThreePointsMade(), "三分");
        validateTheoreticalPoints(item.getPoints(), item.getFieldGoalsMade(), item.getThreePointsMade(),
                item.getFreeThrowsMade(), "球员");
    }

    private void validateShooting(Integer made, Integer attempted, String label) {
        if (nvl(made) > nvl(attempted)) {
            throw new ServiceException(label + "命中数不能大于出手数");
        }
    }

    private void validateThreeWithinFg(Integer fgm, Integer tpm, String label) {
        if (nvl(tpm) > nvl(fgm)) {
            throw new ServiceException(label + "三分命中数不能大于投篮命中数");
        }
    }

    private void validateTheoreticalPoints(Integer points, Integer fgm, Integer tpm, Integer ftm, String label) {
        int theoretical = 2 * (nvl(fgm) - nvl(tpm)) + 3 * nvl(tpm) + nvl(ftm);
        if (nvl(points) != theoretical) {
            throw new ServiceException(label + "得分与投篮数据不一致，理论得分=" + theoretical);
        }
    }

    private BigDecimal computePct(Integer made, Integer attempted) {
        int m = nvl(made);
        int a = nvl(attempted);
        if (a <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(m).divide(BigDecimal.valueOf(a), 4, RoundingMode.HALF_UP);
    }

    private PlayerMatchDataSubmissionDetailResp buildBaseDetailResp(BasketMatch match, List<Long> captainTeamIds) {
        Map<Long, String> teamNames = loadTeamNames(Collections.singletonList(match));
        PlayerMatchDataSubmissionDetailResp resp = new PlayerMatchDataSubmissionDetailResp();
        resp.setMatchId(String.valueOf(match.getId()));
        resp.setMatchNo(match.getMatchNo());
        resp.setSeasonId(String.valueOf(match.getSeasonId()));
        resp.setStageId(String.valueOf(match.getStageId()));
        resp.setHomeTeamId(String.valueOf(match.getHomeTeamId()));
        resp.setHomeTeamName(teamNames.get(match.getHomeTeamId()));
        resp.setAwayTeamId(String.valueOf(match.getAwayTeamId()));
        resp.setAwayTeamName(teamNames.get(match.getAwayTeamId()));
        resp.setMatchStatus(match.getMatchStatus());
        resp.setDataStatus(match.getDataStatus());
        resp.setScheduledStartTime(match.getScheduledStartTime());
        resp.setCaptainTeamIds(captainTeamIds.stream().map(String::valueOf).collect(Collectors.toList()));
        resp.setRosters(loadRosterItems(match.getId()));
        resp.setHomeScore(match.getHomeScore());
        resp.setAwayScore(match.getAwayScore());
        return resp;
    }

    private void fillSubmissionDetail(PlayerMatchDataSubmissionDetailResp resp, BasketMatchDataSubmission submission) {
        resp.setSubmissionId(String.valueOf(submission.getId()));
        resp.setSubmissionNo(submission.getSubmissionNo());
        resp.setVersionNo(submission.getVersionNo());
        resp.setSubmissionStatus(submission.getSubmissionStatus());
        resp.setRejectReason(submission.getRejectReason());
        resp.setHomeScore(submission.getHomeScore());
        resp.setAwayScore(submission.getAwayScore());
        resp.setRemark(submission.getRemark());

        List<BasketMatchDataSubmissionPeriod> periods = submissionPeriodDao.selectList(
                new LambdaQueryWrapper<BasketMatchDataSubmissionPeriod>()
                        .eq(BasketMatchDataSubmissionPeriod::getSubmissionId, submission.getId())
                        .eq(BasketMatchDataSubmissionPeriod::getIsDelete, NOT_DELETED)
                        .orderByAsc(BasketMatchDataSubmissionPeriod::getSortNo)
                        .orderByAsc(BasketMatchDataSubmissionPeriod::getPeriodNo));
        resp.setPeriodScores(periods.stream().map(p -> {
            PlayerMatchDataSubmitReq.PeriodScoreItem item = new PlayerMatchDataSubmitReq.PeriodScoreItem();
            item.setPeriodNo(p.getPeriodNo());
            item.setPeriodType(p.getPeriodType());
            item.setHomeScore(p.getHomeScore());
            item.setAwayScore(p.getAwayScore());
            return item;
        }).collect(Collectors.toList()));

        List<BasketMatchDataSubmissionTeamStat> teamStats = submissionTeamStatDao.selectList(
                new LambdaQueryWrapper<BasketMatchDataSubmissionTeamStat>()
                        .eq(BasketMatchDataSubmissionTeamStat::getSubmissionId, submission.getId())
                        .eq(BasketMatchDataSubmissionTeamStat::getIsDelete, NOT_DELETED)
                        .orderByAsc(BasketMatchDataSubmissionTeamStat::getSortNo));
        resp.setTeamStats(teamStats.stream().map(this::toTeamStatItem).collect(Collectors.toList()));

        List<BasketMatchDataSubmissionPlayerStat> playerStats = submissionPlayerStatDao.selectList(
                new LambdaQueryWrapper<BasketMatchDataSubmissionPlayerStat>()
                        .eq(BasketMatchDataSubmissionPlayerStat::getSubmissionId, submission.getId())
                        .eq(BasketMatchDataSubmissionPlayerStat::getIsDelete, NOT_DELETED)
                        .orderByAsc(BasketMatchDataSubmissionPlayerStat::getSortNo));
        resp.setPlayerStats(playerStats.stream().map(this::toPlayerStatItem).collect(Collectors.toList()));

        List<BasketMatchDataSubmissionMaterial> materials = submissionMaterialDao.selectList(
                new LambdaQueryWrapper<BasketMatchDataSubmissionMaterial>()
                        .eq(BasketMatchDataSubmissionMaterial::getSubmissionId, submission.getId())
                        .eq(BasketMatchDataSubmissionMaterial::getIsDelete, NOT_DELETED)
                        .orderByAsc(BasketMatchDataSubmissionMaterial::getSortNo));
        resp.setMaterialFileIds(materials.stream().map(m -> String.valueOf(m.getFileId())).collect(Collectors.toList()));
    }

    private PlayerMatchDataSubmitReq.TeamStatItem toTeamStatItem(BasketMatchDataSubmissionTeamStat stat) {
        PlayerMatchDataSubmitReq.TeamStatItem item = new PlayerMatchDataSubmitReq.TeamStatItem();
        item.setTeamId(String.valueOf(stat.getTeamId()));
        item.setPoints(stat.getPoints());
        item.setRebounds(stat.getRebounds());
        item.setOffensiveRebounds(stat.getOffensiveRebounds());
        item.setDefensiveRebounds(stat.getDefensiveRebounds());
        item.setAssists(stat.getAssists());
        item.setSteals(stat.getSteals());
        item.setBlocks(stat.getBlocks());
        item.setTurnovers(stat.getTurnovers());
        item.setFouls(stat.getFouls());
        item.setFieldGoalsMade(stat.getFieldGoalsMade());
        item.setFieldGoalsAttempted(stat.getFieldGoalsAttempted());
        item.setThreePointsMade(stat.getThreePointsMade());
        item.setThreePointsAttempted(stat.getThreePointsAttempted());
        item.setFreeThrowsMade(stat.getFreeThrowsMade());
        item.setFreeThrowsAttempted(stat.getFreeThrowsAttempted());
        item.setPossessions(stat.getPossessions());
        item.setEfficiency(stat.getEfficiency());
        return item;
    }

    private PlayerMatchDataSubmitReq.PlayerStatItem toPlayerStatItem(BasketMatchDataSubmissionPlayerStat stat) {
        PlayerMatchDataSubmitReq.PlayerStatItem item = new PlayerMatchDataSubmitReq.PlayerStatItem();
        item.setTeamId(String.valueOf(stat.getTeamId()));
        item.setPlayerId(String.valueOf(stat.getPlayerId()));
        if (stat.getRosterId() != null) {
            item.setRosterId(String.valueOf(stat.getRosterId()));
        }
        item.setStartedFlag(stat.getStartedFlag());
        item.setMinutesSeconds(stat.getMinutesSeconds());
        item.setPoints(stat.getPoints());
        item.setRebounds(stat.getRebounds());
        item.setOffensiveRebounds(stat.getOffensiveRebounds());
        item.setDefensiveRebounds(stat.getDefensiveRebounds());
        item.setAssists(stat.getAssists());
        item.setSteals(stat.getSteals());
        item.setBlocks(stat.getBlocks());
        item.setTurnovers(stat.getTurnovers());
        item.setFouls(stat.getFouls());
        item.setFieldGoalsMade(stat.getFieldGoalsMade());
        item.setFieldGoalsAttempted(stat.getFieldGoalsAttempted());
        item.setThreePointsMade(stat.getThreePointsMade());
        item.setThreePointsAttempted(stat.getThreePointsAttempted());
        item.setFreeThrowsMade(stat.getFreeThrowsMade());
        item.setFreeThrowsAttempted(stat.getFreeThrowsAttempted());
        item.setPlusMinus(stat.getPlusMinus());
        item.setEfficiency(stat.getEfficiency());
        return item;
    }

    private PlayerMatchTaskResp toTaskResp(BasketMatch match, List<Long> myTeamIds, Map<Long, String> teamNameMap,
                                           BasketMatchDataSubmission submission, Map<Long, Long> captainTeamMap) {
        Long myTeamId = null;
        if (myTeamIds.contains(match.getHomeTeamId())) {
            myTeamId = match.getHomeTeamId();
        } else if (myTeamIds.contains(match.getAwayTeamId())) {
            myTeamId = match.getAwayTeamId();
        }

        PlayerMatchTaskResp resp = new PlayerMatchTaskResp();
        resp.setMatchId(String.valueOf(match.getId()));
        resp.setMatchNo(match.getMatchNo());
        resp.setSeasonId(String.valueOf(match.getSeasonId()));
        resp.setStageId(String.valueOf(match.getStageId()));
        resp.setHomeTeamId(String.valueOf(match.getHomeTeamId()));
        resp.setHomeTeamName(teamNameMap.get(match.getHomeTeamId()));
        resp.setAwayTeamId(String.valueOf(match.getAwayTeamId()));
        resp.setAwayTeamName(teamNameMap.get(match.getAwayTeamId()));
        resp.setScheduledStartTime(match.getScheduledStartTime());
        resp.setMatchStatus(match.getMatchStatus());
        resp.setMatchStatusText(resolveMatchStatusText(match.getMatchStatus()));
        resp.setDataStatus(match.getDataStatus());
        resp.setDataStatusText(resolveDataStatusText(match.getDataStatus()));
        resp.setHomeScore(match.getHomeScore());
        resp.setAwayScore(match.getAwayScore());
        if (myTeamId != null) {
            resp.setMyTeamId(String.valueOf(myTeamId));
            resp.setMyTeamName(teamNameMap.get(myTeamId));
        }
        boolean isCaptain = myTeamId != null && myTeamId.equals(captainTeamMap.get(match.getId()));
        resp.setIsCaptain(isCaptain);

        if (submission != null) {
            resp.setSubmissionStatus(submission.getSubmissionStatus());
            resp.setSubmissionStatusText(MatchDataSubmissionStatusEnum.fromCode(submission.getSubmissionStatus()).getDescription());
            resp.setSubmissionId(String.valueOf(submission.getId()));
            resp.setSubmissionNo(submission.getSubmissionNo());
            resp.setVersionNo(submission.getVersionNo());
        }

        boolean finished = Objects.equals(match.getMatchStatus(), BasketPublicConstants.MATCH_FINISHED);
        boolean activeSubmission = submission != null && MatchDataSubmissionStatusEnum.fromCode(submission.getSubmissionStatus()).isActive();
        boolean rejected = submission != null && Objects.equals(submission.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode());
        resp.setCanSubmit(isCaptain && finished && !activeSubmission
                && (match.getDataStatus() == null || match.getDataStatus() != BasketPublicConstants.DATA_CONFIRMED));
        resp.setCanResubmit(isCaptain && finished && rejected
                && submission != null && Objects.equals(submission.getSubmitterTeamId(), myTeamId));
        resp.setCanViewStatus(submission != null);
        resp.setStatusText(buildTaskStatusText(match, submission));
        resp.setActionText(buildTaskActionText(match, submission, resp.getCanSubmit(), resp.getCanResubmit()));
        if (submission != null && Objects.equals(submission.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode())) {
            resp.setRejectReason(submission.getRejectReason());
        }
        return resp;
    }

    private void fillSubmitAvailability(PlayerMatchDataSubmissionDetailResp resp, BasketMatch match) {
        BasketMatchDataSubmission latest = findLatestSubmission(match.getId());
        boolean activeSubmission = latest != null
                && MatchDataSubmissionStatusEnum.fromCode(latest.getSubmissionStatus()).isActive();
        boolean confirmed = match.getDataStatus() != null
                && match.getDataStatus() == BasketPublicConstants.DATA_CONFIRMED;
        if (activeSubmission) {
            resp.setCanSubmit(false);
            resp.setCannotSubmitReason("该比赛已有待审核或已通过的数据提交");
        } else if (confirmed) {
            resp.setCanSubmit(false);
            resp.setCannotSubmitReason("比赛数据已确认，无法提交");
        } else {
            resp.setCanSubmit(true);
            resp.setCannotSubmitReason(null);
        }
    }

    private String buildTaskStatusText(BasketMatch match, BasketMatchDataSubmission submission) {
        if (submission != null) {
            return MatchDataSubmissionStatusEnum.fromCode(submission.getSubmissionStatus()).getDescription();
        }
        return resolveDataStatusText(match.getDataStatus());
    }

    private String buildTaskActionText(BasketMatch match, BasketMatchDataSubmission submission,
                                       Boolean canSubmit, Boolean canResubmit) {
        if (Boolean.TRUE.equals(canSubmit)) {
            return "提交比赛数据";
        }
        if (Boolean.TRUE.equals(canResubmit)) {
            return "修改并重新提交";
        }
        if (submission != null) {
            if (Objects.equals(submission.getSubmissionStatus(), MatchDataSubmissionStatusEnum.APPROVED.getCode())) {
                return "查看公开数据";
            }
            return "查看审核状态";
        }
        if (!Objects.equals(match.getMatchStatus(), BasketPublicConstants.MATCH_FINISHED)) {
            return "查看赛程";
        }
        return "查看";
    }

    private PlayerMatchDataStatusResp toStatusResp(BasketMatch match, BasketMatchDataSubmission submission, Long memberId) {
        Map<Long, String> teamNames = loadTeamNames(Collections.singletonList(match));
        PlayerMatchDataStatusResp resp = new PlayerMatchDataStatusResp();
        resp.setMatchId(String.valueOf(match.getId()));
        resp.setMatchNo(match.getMatchNo());
        resp.setSubmissionId(String.valueOf(submission.getId()));
        resp.setSubmissionNo(submission.getSubmissionNo());
        resp.setVersionNo(submission.getVersionNo());
        resp.setSubmissionStatus(submission.getSubmissionStatus());
        resp.setSubmissionStatusText(MatchDataSubmissionStatusEnum.fromCode(submission.getSubmissionStatus()).getDescription());
        resp.setAuditStatus(submission.getAuditStatus());
        resp.setAuditStatusText(resolveAuditStatusText(submission.getAuditStatus()));
        resp.setSubmittedTime(submission.getSubmittedTime());
        resp.setReviewedTime(submission.getReviewedTime());
        resp.setReviewedByName(submission.getReviewedByName());
        resp.setRejectReason(submission.getRejectReason());
        resp.setAuditRemark(submission.getAuditRemark());
        resp.setSubmitterTeamId(String.valueOf(submission.getSubmitterTeamId()));
        resp.setSubmitterTeamName(teamNames.get(submission.getSubmitterTeamId()));

        boolean isCaptain = isCaptainOfTeam(memberId, submission.getSeasonId(), submission.getSubmitterTeamId());
        boolean rejected = Objects.equals(submission.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode());
        resp.setCanResubmit(isCaptain && rejected);
        resp.setStatusText(MatchDataSubmissionStatusEnum.fromCode(submission.getSubmissionStatus()).getDescription());
        return resp;
    }

    private List<PlayerMatchDataSubmitReq.PeriodScoreItem> defaultRegularPeriods() {
        List<PlayerMatchDataSubmitReq.PeriodScoreItem> periods = new ArrayList<>();
        for (int i = 1; i <= REQUIRED_REGULAR_PERIODS; i++) {
            PlayerMatchDataSubmitReq.PeriodScoreItem item = new PlayerMatchDataSubmitReq.PeriodScoreItem();
            item.setPeriodNo(i);
            item.setPeriodType(MatchPeriodTypeEnum.REGULAR.getCode());
            item.setHomeScore(0);
            item.setAwayScore(0);
            periods.add(item);
        }
        return periods;
    }

    private List<PlayerMatchDataSubmitReq.TeamStatItem> defaultTeamStats(BasketMatch match) {
        List<PlayerMatchDataSubmitReq.TeamStatItem> stats = new ArrayList<>();
        stats.add(emptyTeamStatItem(match.getHomeTeamId()));
        stats.add(emptyTeamStatItem(match.getAwayTeamId()));
        return stats;
    }

    private PlayerMatchDataSubmitReq.TeamStatItem emptyTeamStatItem(Long teamId) {
        PlayerMatchDataSubmitReq.TeamStatItem item = new PlayerMatchDataSubmitReq.TeamStatItem();
        item.setTeamId(String.valueOf(teamId));
        item.setPoints(0);
        item.setRebounds(0);
        item.setOffensiveRebounds(0);
        item.setDefensiveRebounds(0);
        item.setAssists(0);
        item.setSteals(0);
        item.setBlocks(0);
        item.setTurnovers(0);
        item.setFouls(0);
        item.setFieldGoalsMade(0);
        item.setFieldGoalsAttempted(0);
        item.setThreePointsMade(0);
        item.setThreePointsAttempted(0);
        item.setFreeThrowsMade(0);
        item.setFreeThrowsAttempted(0);
        item.setPossessions(BigDecimal.ZERO);
        item.setEfficiency(BigDecimal.ZERO);
        return item;
    }

    private List<PlayerMatchDataSubmitReq.PlayerStatItem> buildPlayerStatTemplate(Long matchId) {
        List<BasketMatchRoster> rosters = matchRosterDao.selectList(new LambdaQueryWrapper<BasketMatchRoster>()
                .eq(BasketMatchRoster::getMatchId, matchId)
                .eq(BasketMatchRoster::getIsDelete, NOT_DELETED));
        List<PlayerMatchDataSubmitReq.PlayerStatItem> items = new ArrayList<>();
        for (BasketMatchRoster roster : rosters) {
            PlayerMatchDataSubmitReq.PlayerStatItem item = new PlayerMatchDataSubmitReq.PlayerStatItem();
            item.setTeamId(String.valueOf(roster.getTeamId()));
            item.setPlayerId(String.valueOf(roster.getPlayerId()));
            item.setRosterId(String.valueOf(roster.getId()));
            item.setStartedFlag(roster.getIsStarting());
            item.setMinutesSeconds(0);
            item.setPoints(0);
            item.setRebounds(0);
            item.setOffensiveRebounds(0);
            item.setDefensiveRebounds(0);
            item.setAssists(0);
            item.setSteals(0);
            item.setBlocks(0);
            item.setTurnovers(0);
            item.setFouls(0);
            item.setFieldGoalsMade(0);
            item.setFieldGoalsAttempted(0);
            item.setThreePointsMade(0);
            item.setThreePointsAttempted(0);
            item.setFreeThrowsMade(0);
            item.setFreeThrowsAttempted(0);
            item.setPlusMinus(0);
            item.setEfficiency(BigDecimal.ZERO);
            items.add(item);
        }
        return items;
    }

    private List<PlayerMatchDataSubmissionDetailResp.RosterItem> loadRosterItems(Long matchId) {
        List<BasketMatchRoster> rosters = matchRosterDao.selectList(new LambdaQueryWrapper<BasketMatchRoster>()
                .eq(BasketMatchRoster::getMatchId, matchId)
                .eq(BasketMatchRoster::getIsDelete, NOT_DELETED));
        List<PlayerMatchDataSubmissionDetailResp.RosterItem> items = new ArrayList<>();
        for (BasketMatchRoster roster : rosters) {
            PlayerMatchDataSubmissionDetailResp.RosterItem item = new PlayerMatchDataSubmissionDetailResp.RosterItem();
            item.setRosterId(String.valueOf(roster.getId()));
            item.setTeamId(String.valueOf(roster.getTeamId()));
            item.setPlayerId(String.valueOf(roster.getPlayerId()));
            item.setJerseyNo(roster.getJerseyNo());
            item.setIsStarting(roster.getIsStarting());
            item.setRosterRole(roster.getRosterRole());
            items.add(item);
        }
        return items;
    }

    private Map<Long, Set<Long>> loadRosterPlayerMap(Long matchId) {
        List<BasketMatchRoster> rosters = matchRosterDao.selectList(new LambdaQueryWrapper<BasketMatchRoster>()
                .eq(BasketMatchRoster::getMatchId, matchId)
                .eq(BasketMatchRoster::getIsDelete, NOT_DELETED));
        Map<Long, Set<Long>> map = new HashMap<>();
        for (BasketMatchRoster roster : rosters) {
            map.computeIfAbsent(roster.getTeamId(), k -> new HashSet<>()).add(roster.getPlayerId());
        }
        return map;
    }

    private List<Long> findActiveTeamIds(Long memberId, Long seasonId) {
        LambdaQueryWrapper<BasketTournamentSeasonRoster> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketTournamentSeasonRoster::getMemberId, memberId)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED);
        if (seasonId != null) {
            wrapper.eq(BasketTournamentSeasonRoster::getSeasonId, seasonId);
        }
        return seasonRosterDao.selectList(wrapper).stream()
                .map(BasketTournamentSeasonRoster::getTeamId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private void assertMemberInSeason(Long memberId, Long seasonId) {
        LambdaQueryWrapper<BasketTournamentSeasonRoster> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketTournamentSeasonRoster::getMemberId, memberId)
                .eq(BasketTournamentSeasonRoster::getSeasonId, seasonId)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED);
        if (seasonRosterDao.selectCount(wrapper) <= 0) {
            throw new ServiceException("您未参与该赛季比赛", 403);
        }
    }

    private void assertMemberCanViewMatch(Long memberId, BasketMatch match) {
        List<Long> myTeamIds = findActiveTeamIds(memberId, match.getSeasonId());
        if (!myTeamIds.contains(match.getHomeTeamId()) && !myTeamIds.contains(match.getAwayTeamId())) {
            throw new ServiceException("无权查看该比赛数据", 403);
        }
    }

    private List<Long> findCaptainTeamIdsForMatch(Long memberId, BasketMatch match) {
        List<Long> result = new ArrayList<>();
        if (isCaptainOfTeam(memberId, match.getSeasonId(), match.getHomeTeamId())) {
            result.add(match.getHomeTeamId());
        }
        if (isCaptainOfTeam(memberId, match.getSeasonId(), match.getAwayTeamId())) {
            result.add(match.getAwayTeamId());
        }
        return result;
    }

    private Map<Long, Long> loadCaptainTeamMap(Long memberId, List<BasketMatch> matches) {
        Map<Long, Long> map = new HashMap<>();
        for (BasketMatch match : matches) {
            if (isCaptainOfTeam(memberId, match.getSeasonId(), match.getHomeTeamId())) {
                map.put(match.getId(), match.getHomeTeamId());
            } else if (isCaptainOfTeam(memberId, match.getSeasonId(), match.getAwayTeamId())) {
                map.put(match.getId(), match.getAwayTeamId());
            }
        }
        return map;
    }

    private boolean isCaptainOfTeam(Long memberId, Long seasonId, Long teamId) {
        LambdaQueryWrapper<BasketTournamentRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketTournamentRegistration::getSeasonId, seasonId)
                .eq(BasketTournamentRegistration::getTeamId, teamId)
                .eq(BasketTournamentRegistration::getCaptainMemberId, memberId)
                .eq(BasketTournamentRegistration::getRegistrationStatus, TournamentRegistrationStatusEnum.APPROVED.getCode())
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED);
        return registrationDao.selectCount(wrapper) > 0;
    }

    private void assertCaptainOfTeam(Long memberId, Long seasonId, Long teamId) {
        if (!isCaptainOfTeam(memberId, seasonId, teamId)) {
            throw new ServiceException("仅该球队队长可提交比赛数据", 403);
        }
    }

    private void assertNoActiveSubmission(Long matchId) {
        LambdaQueryWrapper<BasketMatchDataSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchDataSubmission::getMatchId, matchId)
                .eq(BasketMatchDataSubmission::getIsDelete, NOT_DELETED)
                .in(BasketMatchDataSubmission::getSubmissionStatus,
                        MatchDataSubmissionStatusEnum.PENDING.getCode(),
                        MatchDataSubmissionStatusEnum.APPROVED.getCode());
        if (submissionDao.selectCount(wrapper) > 0) {
            throw new ServiceException("该比赛已有提交中的数据，请勿重复提交");
        }
    }

    private BasketMatchDataSubmission findLatestSubmission(Long matchId) {
        LambdaQueryWrapper<BasketMatchDataSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchDataSubmission::getMatchId, matchId)
                .eq(BasketMatchDataSubmission::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchDataSubmission::getVersionNo)
                .orderByDesc(BasketMatchDataSubmission::getSubmittedTime)
                .last("LIMIT 1");
        return submissionDao.selectOne(wrapper);
    }

    private Map<Long, BasketMatchDataSubmission> loadLatestSubmissions(List<Long> matchIds) {
        if (CollectionUtils.isEmpty(matchIds)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<BasketMatchDataSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BasketMatchDataSubmission::getMatchId, matchIds)
                .eq(BasketMatchDataSubmission::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchDataSubmission::getVersionNo)
                .orderByDesc(BasketMatchDataSubmission::getSubmittedTime);
        List<BasketMatchDataSubmission> list = submissionDao.selectList(wrapper);
        Map<Long, BasketMatchDataSubmission> map = new HashMap<>();
        for (BasketMatchDataSubmission submission : list) {
            map.putIfAbsent(submission.getMatchId(), submission);
        }
        return map;
    }

    private Map<Long, String> loadTeamNames(List<BasketMatch> matches) {
        Set<Long> teamIds = new HashSet<>();
        for (BasketMatch match : matches) {
            if (match.getHomeTeamId() != null) {
                teamIds.add(match.getHomeTeamId());
            }
            if (match.getAwayTeamId() != null) {
                teamIds.add(match.getAwayTeamId());
            }
        }
        if (teamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BasketTeam> teams = teamDao.selectBatchIds(teamIds);
        Map<Long, String> map = new HashMap<>();
        for (BasketTeam team : teams) {
            if (team != null && NOT_DELETED.equals(team.getIsDelete())) {
                map.put(team.getId(), team.getTeamName());
            }
        }
        for (BasketMatch match : matches) {
            if (StringUtils.hasText(match.getHomeTeamNameSnapshot())) {
                map.putIfAbsent(match.getHomeTeamId(), match.getHomeTeamNameSnapshot());
            }
            if (StringUtils.hasText(match.getAwayTeamNameSnapshot())) {
                map.putIfAbsent(match.getAwayTeamId(), match.getAwayTeamNameSnapshot());
            }
        }
        return map;
    }

    private BasketMatch requireMatch(Long matchId) {
        if (matchId == null) {
            throw new ServiceException("比赛主键不能为空");
        }
        BasketMatch match = matchDao.selectById(matchId);
        if (match == null || !NOT_DELETED.equals(match.getIsDelete())) {
            throw new ServiceException("比赛不存在");
        }
        return match;
    }

    private void assertMatchFinished(BasketMatch match) {
        if (!Objects.equals(match.getMatchStatus(), BasketPublicConstants.MATCH_FINISHED)) {
            throw new ServiceException("比赛未结束，暂不可提交数据");
        }
    }

    private String generateSubmissionNo() {
        return "MDS-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000);
    }

    private Long parseLongId(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new ServiceException(label + "不能为空");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(label + "格式无效");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private int nvl(Integer value) {
        return value != null ? value : 0;
    }

    private IPage<PlayerMatchTaskResp> emptyPage(int current, int size) {
        Page<PlayerMatchTaskResp> page = new Page<>(current, size, 0);
        page.setRecords(Collections.emptyList());
        return page;
    }

    private String resolveMatchStatusText(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "已编排";
            case 2 -> "进行中";
            case 3 -> "已结束";
            case 4 -> "已延期";
            case 5 -> "已取消";
            default -> "未知";
        };
    }

    private String resolveDataStatusText(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case BasketPublicConstants.DATA_DRAFT -> "草稿";
            case BasketPublicConstants.DATA_SUBMITTED -> "已提交";
            case BasketPublicConstants.DATA_CONFIRMED -> "已确认";
            case BasketPublicConstants.DATA_REOPENED -> "已重开";
            case BasketPublicConstants.DATA_REJECTED -> "已驳回";
            default -> "未知";
        };
    }

    private String resolveAuditStatusText(String auditStatus) {
        if (!StringUtils.hasText(auditStatus)) {
            return null;
        }
        return switch (auditStatus.toUpperCase()) {
            case "PENDING" -> "待审核";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            default -> auditStatus;
        };
    }

    private static class ValidatedSubmissionPayload {
        private int homeScore;
        private int awayScore;
        private Long winnerTeamId;
        private List<BasketMatchDataSubmissionPeriod> periods;
        private List<BasketMatchDataSubmissionTeamStat> teamStats;
        private List<BasketMatchDataSubmissionPlayerStat> playerStats;
        private List<BasketMatchDataSubmissionMaterial> materials;
    }
}
