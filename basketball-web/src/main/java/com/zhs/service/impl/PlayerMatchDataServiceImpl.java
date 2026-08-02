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
import com.zhs.dao.BasketMatchPlayerStatDao;
import com.zhs.dao.BasketMatchRosterDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketStageStandingDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamSeasonProfileDao;
import com.zhs.dao.BasketTeamSeasonStatDao;
import com.zhs.dao.BasketTournamentDao;
import com.zhs.dao.BasketTournamentRegistrationDao;
import com.zhs.dao.BasketTournamentSeasonDao;
import com.zhs.dao.BasketTournamentSeasonRosterDao;
import com.zhs.dao.BasketTournamentStageDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.enums.MatchDataMaterialTypeEnum;
import com.zhs.enums.MatchDataSubmissionStatusEnum;
import com.zhs.enums.MatchPeriodTypeEnum;
import com.zhs.enums.RosterRoleEnum;
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
import com.zhs.model.BasketMatchPlayerStat;
import com.zhs.model.BasketMatchRoster;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketStageStanding;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamSeasonProfile;
import com.zhs.model.BasketTeamSeasonStat;
import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentRegistration;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.model.BasketTournamentSeasonRoster;
import com.zhs.model.BasketTournamentStage;
import com.zhs.request.matchdata.PlayerMatchDataResubmitReq;
import com.zhs.request.matchdata.PlayerMatchDataSubmitReq;
import com.zhs.request.matchdata.PlayerMatchTeamPlayerStatsResubmitReq;
import com.zhs.request.matchdata.PlayerMatchTeamPlayerStatsSubmitReq;
import com.zhs.response.matchdata.MatchDetailPlayerStatResp;
import com.zhs.response.matchdata.PlayerMatchDataStatusResp;
import com.zhs.response.matchdata.PlayerMatchDataSubmissionDetailResp;
import com.zhs.response.matchdata.PlayerMatchDataSubmitResultResp;
import com.zhs.response.matchdata.PlayerMatchTaskResp;
import com.zhs.response.participation.EventRosterMemberResp;
import com.zhs.service.IAccountFileService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.IMPlayerMatchDataService;
import com.zhs.service.account.AccessibleFile;
import com.zhs.service.support.BasketPublicConstants;
import com.zhs.service.support.BasketPublicSupport;
import com.zhs.service.support.PlayerMatchDisputeExceptionSupport;
import com.zhs.service.support.PlayerMatchDataSubmitSupport;
import com.zhs.support.MemberAvatarSupport;
import com.zhs.support.GameTerminalSupport;
import com.zhs.support.RegistrationRosterRuleSupport;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PlayerMatchDataServiceImpl implements IMPlayerMatchDataService {

    private static final Integer NOT_DELETED = 0;
    private static final String AUDIT_TYPE_RESULT = "RESULT";
    private static final String AUDIT_STATUS_PENDING = "PENDING";
    private static final String SUBMITTER_ROLE_CAPTAIN = "CAPTAIN";
    private static final String SIDE_HOME = "HOME";
    private static final String SIDE_AWAY = "AWAY";
    private static final long STAGE_LEVEL_GROUP_ID = 0L;
    private static final int MATERIAL_STATUS_ACTIVE = 1;
    private static final int REQUIRED_REGULAR_PERIODS = 4;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Pattern GAME_ID_PLATFORM_PREFIX = Pattern.compile(
            "^(PS5?|PC|XBOX(?:\\sONE)?)\\s*[:：]", Pattern.CASE_INSENSITIVE);

    @Resource
    private BasketTournamentSeasonRosterDao seasonRosterDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketTournamentSeasonDao seasonDao;
    @Resource
    private BasketTournamentDao tournamentDao;
    @Resource
    private BasketTournamentStageDao stageDao;
    @Resource
    private BasketTournamentRegistrationDao registrationDao;
    @Resource
    private BasketMatchDao matchDao;
    @Resource
    private BasketMatchRosterDao matchRosterDao;
    @Resource
    private BasketMatchPlayerStatDao matchPlayerStatDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamSeasonProfileDao eventTeamDao;
    @Resource
    private BasketStageStandingDao standingDao;
    @Resource
    private BasketTeamSeasonStatDao teamSeasonStatDao;
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
    @Resource
    private PlayerMatchDisputeExceptionSupport disputeExceptionSupport;
    @Resource
    private PlayerMatchDataSubmitSupport matchDataSubmitSupport;
    @Resource
    private BasketPublicSupport publicSupport;

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
                .eq(BasketMatch::getPublishStatus, BasketPublicConstants.PUBLISH_PUBLISHED)
                .ne(BasketMatch::getMatchStatus, 0)
                .isNotNull(BasketMatch::getScheduleBatchId)
                .isNotNull(BasketMatch::getHomeEventTeamId)
                .isNotNull(BasketMatch::getAwayEventTeamId)
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
        wrapper.orderByAsc(BasketMatch::getScheduledStartTime).orderByAsc(BasketMatch::getSortNo).orderByAsc(BasketMatch::getId);

        Page<BasketMatch> page = matchDao.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> teamNameMap = loadTeamNames(page.getRecords());
        List<Long> matchIdList = page.getRecords().stream().map(BasketMatch::getId).collect(Collectors.toList());
        Map<String, BasketMatchDataSubmission> submissionByTeamMap = loadLatestSubmissionsByTeam(matchIdList);
        Map<Long, Long> captainTeamMap = loadCaptainTeamMap(memberId, page.getRecords());

        Page<PlayerMatchTaskResp> respPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<PlayerMatchTaskResp> records = new ArrayList<>();
        for (BasketMatch match : page.getRecords()) {
            Long myTeamId = resolveMyTeamIdForMatch(match, myTeamIds, null);
            BasketMatchDataSubmission mySubmission = resolveTeamSubmission(submissionByTeamMap, match.getId(), myTeamId);
            records.add(toTaskResp(match, myTeamIds, teamNameMap, mySubmission, submissionByTeamMap, captainTeamMap, null));
        }
        respPage.setRecords(records);
        return respPage;
    }

    @Override
    public List<PlayerMatchTaskResp> buildTaskItemsForEventTeams(Long memberId, Map<Long, Long> matchIdToEventTeamId) {
        if (memberId == null || matchIdToEventTeamId == null || matchIdToEventTeamId.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> matchIds = new ArrayList<>(matchIdToEventTeamId.keySet());
        List<BasketMatch> matches = matchDao.selectList(new LambdaQueryWrapper<BasketMatch>()
                .in(BasketMatch::getId, matchIds)
                .eq(BasketMatch::getIsDelete, NOT_DELETED));
        if (matches.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, BasketMatch> matchMap = matches.stream()
                .collect(Collectors.toMap(BasketMatch::getId, m -> m, (a, b) -> a));
        Map<Long, String> teamNameMap = loadTeamNames(matches);
        Map<String, BasketMatchDataSubmission> submissionByTeamMap = loadLatestSubmissionsByTeam(matchIds);
        Map<Long, Long> captainTeamMap = loadCaptainTeamMap(memberId, matches);

        List<PlayerMatchTaskResp> records = new ArrayList<>();
        for (Long matchId : matchIds) {
            BasketMatch match = matchMap.get(matchId);
            Long eventTeamId = matchIdToEventTeamId.get(matchId);
            if (match == null || eventTeamId == null) {
                continue;
            }
            BasketTeamSeasonProfile eventTeam = eventTeamDao.selectById(eventTeamId);
            List<Long> myTeamIds = eventTeam != null && eventTeam.getTeamId() != null
                    ? Collections.singletonList(eventTeam.getTeamId())
                    : Collections.emptyList();
            Long myTeamId = resolveMyTeamIdForMatch(match, myTeamIds, eventTeamId);
            BasketMatchDataSubmission mySubmission = resolveTeamSubmission(submissionByTeamMap, matchId, myTeamId);
            PlayerMatchTaskResp taskResp = toTaskResp(match, myTeamIds, teamNameMap, mySubmission, submissionByTeamMap,
                    captainTeamMap, eventTeamId);
            enrichScheduleSummary(taskResp, match);
            records.add(taskResp);
        }
        return records;
    }

    @Override
    public IPage<PlayerMatchTaskResp> pageEventTeamMatches(String eventTeamId, Long stageId, Integer dataStatus,
                                                           Integer matchStatus, String beginTime, String endTime,
                                                           Integer pageNum, Integer pageSize) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);

        Long eventTeamPk = parseLongId(eventTeamId, "赛事球队主键");
        BasketTeamSeasonProfile eventTeam = requireEventTeam(eventTeamPk);
        requireEventTeamMember(memberId, eventTeamPk, eventTeam.getSeasonId());

        int current = pageNum == null || pageNum < 1 ? BasketPublicConstants.DEFAULT_PAGE : pageNum;
        int size = pageSize == null || pageSize < 1 ? BasketPublicConstants.DEFAULT_SIZE : Math.min(pageSize, BasketPublicConstants.MAX_SIZE);

        LambdaQueryWrapper<BasketMatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatch::getIsDelete, NOT_DELETED)
                .eq(BasketMatch::getSeasonId, eventTeam.getSeasonId())
                .eq(BasketMatch::getPublishStatus, BasketPublicConstants.PUBLISH_PUBLISHED)
                .ne(BasketMatch::getMatchStatus, 0)
                .isNotNull(BasketMatch::getScheduleBatchId)
                .isNotNull(BasketMatch::getHomeEventTeamId)
                .isNotNull(BasketMatch::getAwayEventTeamId)
                .and(w -> w.eq(BasketMatch::getHomeEventTeamId, eventTeamPk)
                        .or()
                        .eq(BasketMatch::getAwayEventTeamId, eventTeamPk));
        if (stageId != null) {
            wrapper.eq(BasketMatch::getStageId, stageId);
        }
        if (dataStatus != null) {
            wrapper.eq(BasketMatch::getDataStatus, dataStatus);
        }
        if (matchStatus != null) {
            wrapper.eq(BasketMatch::getMatchStatus, matchStatus);
        }
        Date begin = parseDateTime(beginTime);
        Date end = parseDateTime(endTime);
        if (begin != null) {
            wrapper.ge(BasketMatch::getScheduledStartTime, begin);
        }
        if (end != null) {
            wrapper.le(BasketMatch::getScheduledStartTime, end);
        }

        long total = matchDao.selectCount(wrapper);
        if (total == 0) {
            return emptyPage(current, size);
        }

        List<BasketMatch> allMatches = matchDao.selectList(wrapper);
        List<Long> allMatchIds = allMatches.stream().map(BasketMatch::getId).collect(Collectors.toList());
        Map<String, BasketMatchDataSubmission> submissionByTeamMap = loadLatestSubmissionsByTeam(allMatchIds);
        allMatches.sort(this::compareEventTeamMatchesChronological);

        int fromIndex = (current - 1) * size;
        if (fromIndex >= allMatches.size()) {
            Page<PlayerMatchTaskResp> emptyCurrentPage = new Page<>(current, size, total);
            emptyCurrentPage.setRecords(Collections.emptyList());
            emptyCurrentPage.setPages((total + size - 1) / size);
            return emptyCurrentPage;
        }
        int toIndex = Math.min(fromIndex + size, allMatches.size());
        List<BasketMatch> pageMatches = allMatches.subList(fromIndex, toIndex);

        List<Long> myTeamIds = Collections.singletonList(eventTeam.getTeamId());
        Map<Long, String> teamNameMap = loadTeamNames(pageMatches);
        Map<Long, Long> captainTeamMap = loadCaptainTeamMap(memberId, pageMatches);

        Page<PlayerMatchTaskResp> respPage = new Page<>(current, size, total);
        List<PlayerMatchTaskResp> records = new ArrayList<>();
        for (BasketMatch match : pageMatches) {
            Long myTeamId = resolveMyTeamIdForMatch(match, myTeamIds, eventTeamPk);
            BasketMatchDataSubmission mySubmission = resolveTeamSubmission(submissionByTeamMap, match.getId(), myTeamId);
            records.add(toTaskResp(match, myTeamIds, teamNameMap, mySubmission, submissionByTeamMap, captainTeamMap, eventTeamPk));
        }
        respPage.setRecords(records);
        respPage.setPages((total + size - 1) / size);
        return respPage;
    }

    @Override
    public PlayerMatchTaskResp getEventTeamMatchDetail(String eventTeamId, Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);

        Long eventTeamPk = parseLongId(eventTeamId, "赛事球队主键");
        BasketTeamSeasonProfile eventTeam = requireEventTeam(eventTeamPk);
        requireEventTeamMember(memberId, eventTeamPk, eventTeam.getSeasonId());

        BasketMatch match = matchDao.selectById(matchId);
        if (match == null || !NOT_DELETED.equals(match.getIsDelete())) {
            throw new ServiceException("比赛不存在");
        }
        if (!Objects.equals(match.getSeasonId(), eventTeam.getSeasonId())) {
            throw new ServiceException("比赛不属于当前赛事", 403);
        }
        if (!Objects.equals(match.getPublishStatus(), BasketPublicConstants.PUBLISH_PUBLISHED)
                || Objects.equals(match.getMatchStatus(), 0)) {
            throw new ServiceException("比赛尚未发布", 403);
        }
        if (!Objects.equals(match.getHomeEventTeamId(), eventTeamPk)
                && !Objects.equals(match.getAwayEventTeamId(), eventTeamPk)) {
            throw new ServiceException("无权查看该比赛", 403);
        }

        List<Long> myTeamIds = Collections.singletonList(eventTeam.getTeamId());
        Map<Long, String> teamNameMap = loadTeamNames(Collections.singletonList(match));
        Map<String, BasketMatchDataSubmission> submissionByTeamMap = loadLatestSubmissionsByTeam(
                Collections.singletonList(match.getId()));
        Map<Long, Long> captainTeamMap = loadCaptainTeamMap(memberId, Collections.singletonList(match));
        Long myTeamId = resolveMyTeamIdForMatch(match, myTeamIds, eventTeamPk);
        BasketMatchDataSubmission mySubmission = resolveTeamSubmission(submissionByTeamMap, match.getId(), myTeamId);
        PlayerMatchTaskResp resp = toTaskResp(match, myTeamIds, teamNameMap, mySubmission, submissionByTeamMap, captainTeamMap, eventTeamPk);
        enrichMatchDetail(resp, match, eventTeam, mySubmission);
        return resp;
    }

    @Override
    public PlayerMatchDataSubmissionDetailResp getSubmitDetail(Long matchId) {
        Long memberId = UserContext.getRequiredMemberId();
        captainPermissionService.requireCertifiedPlayer(memberId);
        BasketMatch match = requireMatch(matchId);
        matchDataSubmitSupport.assertSubmissionAllowed(match);
        assertMemberInSeason(memberId, match.getSeasonId());

        List<Long> captainTeamIds = findCaptainTeamIdsForMatch(memberId, match);
        if (captainTeamIds.isEmpty()) {
            throw new ServiceException("仅参赛球队队长可提交比赛数据", 403);
        }

        PlayerMatchDataSubmissionDetailResp resp = buildBaseDetailResp(match, captainTeamIds);
        resp.setPeriodScores(defaultRegularPeriods());
        resp.setTeamStats(defaultTeamStats(match));
        resp.setPlayerStats(buildPlayerStatTemplate(match.getId()));
        fillSubmitAvailability(resp, match, captainTeamIds.get(0));
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

        Long myTeamId = resolveMemberTeamIdInMatch(memberId, match);
        BasketMatchDataSubmission submission = findLatestSubmission(matchId, myTeamId);
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

        List<Long> captainTeamIds = findCaptainTeamIdsForMatch(memberId, match);
        BasketMatchDataSubmission submission = findRejectedSubmissionForTeams(matchId, captainTeamIds);
        if (submission == null) {
            throw new ServiceException("仅驳回后可重新提交");
        }
        assertCaptainOfTeam(memberId, match.getSeasonId(), submission.getSubmitterTeamId());

        captainTeamIds = Collections.singletonList(submission.getSubmitterTeamId());
        PlayerMatchDataSubmissionDetailResp resp = buildBaseDetailResp(match, captainTeamIds);
        fillSubmissionDetail(resp, submission);
        resp.setCanSubmit(true);
        resp.setCannotSubmitReason(null);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerMatchDataSubmitResultResp resubmit(Long matchId, PlayerMatchDataResubmitReq req) {
        Long memberId = UserContext.getRequiredMemberId();
        BasketMatch match = requireMatch(matchId);
        List<Long> captainTeamIds = findCaptainTeamIdsForMatch(memberId, match);
        BasketMatchDataSubmission latest = findRejectedSubmissionForTeams(matchId, captainTeamIds);
        if (latest == null) {
            throw new ServiceException("仅驳回后可重新提交");
        }
        assertCaptainOfTeam(memberId, latest.getSeasonId(), latest.getSubmitterTeamId());
        return doSubmit(matchId, req, true, latest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerMatchDataSubmitResultResp submitTeamPlayerStats(Long matchId, PlayerMatchTeamPlayerStatsSubmitReq req) {
        return doSubmitTeamPlayerStats(matchId, req, false, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlayerMatchDataSubmitResultResp resubmitTeamPlayerStats(Long matchId, PlayerMatchTeamPlayerStatsResubmitReq req) {
        Long memberId = UserContext.getRequiredMemberId();
        BasketMatch match = requireMatch(matchId);
        List<Long> captainTeamIds = findCaptainTeamIdsForMatch(memberId, match);
        BasketMatchDataSubmission latest = findRejectedSubmissionForTeams(matchId, captainTeamIds);
        if (latest == null) {
            throw new ServiceException("仅驳回后可重新提交");
        }
        assertCaptainOfTeam(memberId, latest.getSeasonId(), latest.getSubmitterTeamId());
        return doSubmitTeamPlayerStats(matchId, req, true, latest);
    }

    private PlayerMatchDataSubmitResultResp doSubmitTeamPlayerStats(Long matchId,
                                                                    PlayerMatchTeamPlayerStatsSubmitReq req,
                                                                    boolean resubmit,
                                                                    BasketMatchDataSubmission previous) {
        if (req == null || CollectionUtils.isEmpty(req.getPlayerStats())) {
            throw new ServiceException("球员数据不能为空");
        }
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile profile = captainPermissionService.requireCertifiedPlayer(memberId);

        BasketMatch match = matchDao.selectByIdForUpdate(matchId);
        if (match == null || !NOT_DELETED.equals(match.getIsDelete())) {
            throw new ServiceException("比赛不存在");
        }
        matchDataSubmitSupport.assertSubmissionAllowed(match);

        if (resubmit) {
            if (previous == null || !Objects.equals(previous.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode())) {
                throw new ServiceException("仅驳回后可重新提交");
            }
        } else {
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
        } else if (StringUtils.hasText(req.getSubmitterTeamId())) {
            submitterTeamId = parseLongId(req.getSubmitterTeamId(), "提交球队");
            if (!captainTeamIds.contains(submitterTeamId)) {
                throw new ServiceException("仅可提交本队比赛数据", 403);
            }
        } else {
            submitterTeamId = captainTeamIds.get(0);
        }
        if (!submitterTeamId.equals(match.getHomeTeamId()) && !submitterTeamId.equals(match.getAwayTeamId())) {
            throw new ServiceException("提交球队须为本场比赛参赛球队");
        }
        if (!resubmit) {
            assertNoActiveSubmission(matchId, submitterTeamId);
        }
        assertCaptainOfTeam(memberId, match.getSeasonId(), submitterTeamId);

        ValidatedSubmissionPayload payload = validateAndBuildTeamPlayerStatsPayload(match, submitterTeamId, req);
        return persistSubmission(match, profile, memberId, submitterTeamId, payload, resubmit, previous, req.getRemark());
    }

    private PlayerMatchDataSubmitResultResp persistSubmission(BasketMatch match, BasketPlayerProfile profile, Long memberId,
                                                            Long submitterTeamId, ValidatedSubmissionPayload payload,
                                                            boolean resubmit, BasketMatchDataSubmission previous,
                                                            String remark) {
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
        submission.setRemark(trimToNull(remark));
        submission.setCreateTime(now);
        submission.setUpdateTime(now);
        submission.setIsDelete(NOT_DELETED);

        try {
            submissionDao.insert(submission);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("本队已有提交中的数据，请勿重复提交");
        }

        insertChildren(submission, match, payload, now);
        insertAuditRecord(match, submission, memberId, now, resubmit ? "RESUBMIT" : "SUBMIT");

        ensureMatchFinishedIfScheduled(match);

        if (resubmit) {
            BasketMatchDataSubmission superseded = new BasketMatchDataSubmission();
            superseded.setId(previous.getId());
            superseded.setSubmissionStatus(MatchDataSubmissionStatusEnum.SUPERSEDED.getCode());
            superseded.setUpdateTime(now);
            submissionDao.updateById(superseded);
        }

        if (matchDao.updateDataStatusIfAllowed(match.getId(), BasketPublicConstants.DATA_SUBMITTED, NOT_DELETED) != 1
                && !Objects.equals(match.getDataStatus(), BasketPublicConstants.DATA_SUBMITTED)) {
            throw new ServiceException("比赛数据状态不可提交");
        }

        PlayerMatchDataSubmitResultResp resp = new PlayerMatchDataSubmitResultResp();
        resp.setSubmissionId(String.valueOf(submission.getId()));
        resp.setSubmissionNo(submission.getSubmissionNo());
        resp.setVersionNo(submission.getVersionNo());
        return resp;
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
        matchDataSubmitSupport.assertSubmissionAllowed(match);

        if (resubmit) {
            if (previous == null || !Objects.equals(previous.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode())) {
                throw new ServiceException("仅驳回后可重新提交");
            }
        } else {
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
        if (!resubmit) {
            assertNoActiveSubmission(matchId, submitterTeamId);
        }
        assertCaptainOfTeam(memberId, match.getSeasonId(), submitterTeamId);

        ValidatedSubmissionPayload payload = validateAndBuildPayload(match, req);
        return persistSubmission(match, profile, memberId, submitterTeamId, payload, resubmit, previous, req.getRemark());
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

        Map<Long, Set<Long>> rosterPlayerMap = loadEffectiveRosterPlayerMap(match);
        List<BasketMatchDataSubmissionPlayerStat> playerStats = new ArrayList<>();
        Map<Long, int[]> teamAgg = new HashMap<>();
        teamAgg.put(match.getHomeTeamId(), new int[15]);
        teamAgg.put(match.getAwayTeamId(), new int[15]);
        Set<Long> playerIds = new HashSet<>();
        int totalPlayerPoints = 0;
        for (PlayerMatchDataSubmitReq.PlayerStatItem item : req.getPlayerStats()) {
            Long teamId = parseLongId(item.getTeamId(), "球队");
            Long playerId = parseLongId(item.getPlayerId(), "球员");
            if (!playerIds.add(playerId)) {
                throw new ServiceException("同一球员不能重复录入");
            }
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
            agg[2] += nvl(item.getOffensiveRebounds());
            agg[3] += nvl(item.getDefensiveRebounds());
            agg[4] += nvl(item.getAssists());
            agg[5] += nvl(item.getSteals());
            agg[6] += nvl(item.getBlocks());
            agg[7] += nvl(item.getTurnovers());
            agg[8] += nvl(item.getFouls());
            agg[9] += nvl(item.getFieldGoalsMade());
            agg[10] += nvl(item.getFieldGoalsAttempted());
            agg[11] += nvl(item.getThreePointsMade());
            agg[12] += nvl(item.getThreePointsAttempted());
            agg[13] += nvl(item.getFreeThrowsMade());
            agg[14] += nvl(item.getFreeThrowsAttempted());
        }
        if (totalPlayerPoints != homeScore + awayScore) {
            throw new ServiceException("球员得分合计须等于比赛总得分");
        }
        validateTeamPlayerAggregate(teamStatMap, teamAgg);

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

    private ValidatedSubmissionPayload validateAndBuildTeamPlayerStatsPayload(BasketMatch match, Long submitterTeamId,
                                                                              PlayerMatchTeamPlayerStatsSubmitReq req) {
        Long opponentTeamId = Objects.equals(submitterTeamId, match.getHomeTeamId())
                ? match.getAwayTeamId() : match.getHomeTeamId();
        if (opponentTeamId == null) {
            throw new ServiceException("比赛对手球队信息不完整");
        }

        Map<Long, Set<Long>> rosterPlayerMap = loadEffectiveRosterPlayerMap(match);
        Set<Long> submitterRoster = rosterPlayerMap.get(submitterTeamId);
        Set<Long> opponentRoster = rosterPlayerMap.getOrDefault(opponentTeamId, Collections.emptySet());
        if (CollectionUtils.isEmpty(submitterRoster)) {
            throw new ServiceException("本队出场名单为空，无法提交");
        }

        Map<Long, PlayerMatchTeamPlayerStatsSubmitReq.PlayerStatItem> submitterStatMap = new HashMap<>();
        for (PlayerMatchTeamPlayerStatsSubmitReq.PlayerStatItem item : req.getPlayerStats()) {
            if (item == null || !StringUtils.hasText(item.getPlayerId())) {
                throw new ServiceException("球员主键不能为空");
            }
            Long playerId = parseLongId(item.getPlayerId(), "球员");
            if (!submitterRoster.contains(playerId)) {
                throw new ServiceException("只能提交本队出场球员数据");
            }
            if (submitterStatMap.putIfAbsent(playerId, item) != null) {
                throw new ServiceException("同一球员不能重复录入");
            }
            validateSimpleTeamPlayerStatItem(item);
        }
        if (submitterStatMap.size() != submitterRoster.size()) {
            throw new ServiceException("须为本队全部出场球员录入数据");
        }

        List<BasketMatchDataSubmissionPlayerStat> playerStats = new ArrayList<>();
        Map<Long, int[]> teamAgg = new HashMap<>();
        teamAgg.put(submitterTeamId, new int[15]);
        teamAgg.put(opponentTeamId, new int[15]);

        for (PlayerMatchTeamPlayerStatsSubmitReq.PlayerStatItem item : submitterStatMap.values()) {
            Long playerId = parseLongId(item.getPlayerId(), "球员");
            PlayerMatchDataSubmitReq.PlayerStatItem fullItem = toFullPlayerStatItem(submitterTeamId, item);
            playerStats.add(buildPlayerStat(match, fullItem, submitterTeamId, playerId));
            accumulatePlayerAgg(teamAgg.get(submitterTeamId), fullItem);
        }

        for (Long opponentPlayerId : opponentRoster) {
            PlayerMatchDataSubmitReq.PlayerStatItem zeroItem = zeroPlayerStatItem(opponentTeamId, opponentPlayerId);
            playerStats.add(buildPlayerStat(match, zeroItem, opponentTeamId, opponentPlayerId));
        }

        int submitterPoints = teamAgg.get(submitterTeamId)[0];
        int homeScore;
        int awayScore;
        boolean hasOfficialScore = hasOfficialMatchScore(match);
        if (hasOfficialScore) {
            homeScore = nvl(match.getHomeScore());
            awayScore = nvl(match.getAwayScore());
            int expectedTeamScore = Objects.equals(submitterTeamId, match.getHomeTeamId()) ? homeScore : awayScore;
            if (submitterPoints != expectedTeamScore) {
                throw new ServiceException("本队球员得分合计须与比赛比分一致");
            }
        } else if (Objects.equals(submitterTeamId, match.getHomeTeamId())) {
            homeScore = submitterPoints;
            awayScore = 0;
        } else {
            homeScore = 0;
            awayScore = submitterPoints;
        }

        Long winnerTeamId = null;
        if (homeScore > awayScore) {
            winnerTeamId = match.getHomeTeamId();
        } else if (awayScore > homeScore) {
            winnerTeamId = match.getAwayTeamId();
        }

        List<BasketMatchDataSubmissionPeriod> periods = buildAutoRegularPeriods(homeScore, awayScore);
        List<BasketMatchDataSubmissionTeamStat> teamStats = new ArrayList<>();
        teamStats.add(buildAggregatedTeamStat(match, submitterTeamId, opponentTeamId, teamAgg.get(submitterTeamId)));
        teamStats.add(buildOpponentTeamStatShell(match, opponentTeamId, submitterTeamId,
                Objects.equals(opponentTeamId, match.getHomeTeamId()) ? homeScore : awayScore));

        validateSubmitterTeamPlayerAggregate(teamStats, teamAgg, submitterTeamId);

        ValidatedSubmissionPayload payload = new ValidatedSubmissionPayload();
        payload.homeScore = homeScore;
        payload.awayScore = awayScore;
        payload.winnerTeamId = winnerTeamId;
        payload.periods = periods;
        payload.teamStats = teamStats;
        payload.playerStats = playerStats;
        payload.materials = Collections.emptyList();
        return payload;
    }

    private boolean hasOfficialMatchScore(BasketMatch match) {
        if (match.getHomeScore() == null || match.getAwayScore() == null) {
            return false;
        }
        return match.getHomeScore() > 0 || match.getAwayScore() > 0;
    }

    private void validateSimpleTeamPlayerStatItem(PlayerMatchTeamPlayerStatsSubmitReq.PlayerStatItem item) {
        validateNonNegativeStat(item.getPoints(), "得分");
        validateNonNegativeStat(item.getRebounds(), "篮板");
        validateNonNegativeStat(item.getAssists(), "助攻");
        validateNonNegativeStat(item.getSteals(), "抢断");
        validateNonNegativeStat(item.getBlocks(), "盖帽");
        validateNonNegativeStat(item.getTurnovers(), "失误");
        validateNonNegativeStat(item.getFouls(), "犯规");
        if (nvl(item.getFouls()) > 6) {
            throw new ServiceException("犯规数不能超过6");
        }
    }

    private void validateNonNegativeStat(Integer value, String label) {
        if (value != null && value < 0) {
            throw new ServiceException(label + "不能为负数");
        }
    }

    private PlayerMatchDataSubmitReq.PlayerStatItem toFullPlayerStatItem(Long teamId,
                                                                         PlayerMatchTeamPlayerStatsSubmitReq.PlayerStatItem item) {
        int points = nvl(item.getPoints());
        PlayerMatchDataSubmitReq.PlayerStatItem full = new PlayerMatchDataSubmitReq.PlayerStatItem();
        full.setTeamId(String.valueOf(teamId));
        full.setPlayerId(item.getPlayerId());
        full.setRosterId(item.getRosterId());
        full.setPoints(points);
        full.setRebounds(nvl(item.getRebounds()));
        full.setOffensiveRebounds(0);
        full.setDefensiveRebounds(nvl(item.getRebounds()));
        full.setAssists(nvl(item.getAssists()));
        full.setSteals(nvl(item.getSteals()));
        full.setBlocks(nvl(item.getBlocks()));
        full.setTurnovers(nvl(item.getTurnovers()));
        full.setFouls(nvl(item.getFouls()));
        full.setFieldGoalsMade(0);
        full.setFieldGoalsAttempted(0);
        full.setThreePointsMade(0);
        full.setThreePointsAttempted(0);
        full.setFreeThrowsMade(0);
        full.setFreeThrowsAttempted(0);
        full.setStartedFlag(0);
        full.setMinutesSeconds(0);
        full.setPlusMinus(0);
        full.setEfficiency(BigDecimal.ZERO);
        return full;
    }

    private PlayerMatchDataSubmitReq.PlayerStatItem zeroPlayerStatItem(Long teamId, Long playerId) {
        PlayerMatchDataSubmitReq.PlayerStatItem item = new PlayerMatchDataSubmitReq.PlayerStatItem();
        item.setTeamId(String.valueOf(teamId));
        item.setPlayerId(String.valueOf(playerId));
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
        item.setStartedFlag(0);
        item.setMinutesSeconds(0);
        item.setPlusMinus(0);
        item.setEfficiency(BigDecimal.ZERO);
        return item;
    }

    private void accumulatePlayerAgg(int[] agg, PlayerMatchDataSubmitReq.PlayerStatItem item) {
        agg[0] += nvl(item.getPoints());
        agg[1] += nvl(item.getRebounds());
        agg[2] += nvl(item.getOffensiveRebounds());
        agg[3] += nvl(item.getDefensiveRebounds());
        agg[4] += nvl(item.getAssists());
        agg[5] += nvl(item.getSteals());
        agg[6] += nvl(item.getBlocks());
        agg[7] += nvl(item.getTurnovers());
        agg[8] += nvl(item.getFouls());
        agg[9] += nvl(item.getFieldGoalsMade());
        agg[10] += nvl(item.getFieldGoalsAttempted());
        agg[11] += nvl(item.getThreePointsMade());
        agg[12] += nvl(item.getThreePointsAttempted());
        agg[13] += nvl(item.getFreeThrowsMade());
        agg[14] += nvl(item.getFreeThrowsAttempted());
    }

    private List<BasketMatchDataSubmissionPeriod> buildAutoRegularPeriods(int homeScore, int awayScore) {
        int[] homeQuarters = splitScoreAcrossPeriods(homeScore, REQUIRED_REGULAR_PERIODS);
        int[] awayQuarters = splitScoreAcrossPeriods(awayScore, REQUIRED_REGULAR_PERIODS);
        List<BasketMatchDataSubmissionPeriod> periods = new ArrayList<>();
        for (int i = 0; i < REQUIRED_REGULAR_PERIODS; i++) {
            BasketMatchDataSubmissionPeriod period = new BasketMatchDataSubmissionPeriod();
            period.setPeriodNo(i + 1);
            period.setPeriodType(MatchPeriodTypeEnum.REGULAR.getCode());
            period.setHomeScore(homeQuarters[i]);
            period.setAwayScore(awayQuarters[i]);
            periods.add(period);
        }
        return periods;
    }

    private int[] splitScoreAcrossPeriods(int total, int periods) {
        int[] result = new int[periods];
        int base = total / periods;
        int remainder = total % periods;
        for (int i = 0; i < periods; i++) {
            result[i] = base + (i < remainder ? 1 : 0);
        }
        return result;
    }

    private BasketMatchDataSubmissionTeamStat buildAggregatedTeamStat(BasketMatch match, Long teamId, Long opponentTeamId,
                                                                      int[] agg) {
        PlayerMatchDataSubmitReq.TeamStatItem item = new PlayerMatchDataSubmitReq.TeamStatItem();
        item.setTeamId(String.valueOf(teamId));
        item.setPoints(agg[0]);
        item.setRebounds(agg[1]);
        item.setOffensiveRebounds(agg[2]);
        item.setDefensiveRebounds(agg[3]);
        item.setAssists(agg[4]);
        item.setSteals(agg[5]);
        item.setBlocks(agg[6]);
        item.setTurnovers(agg[7]);
        item.setFouls(agg[8]);
        item.setFieldGoalsMade(agg[9]);
        item.setFieldGoalsAttempted(agg[10]);
        item.setThreePointsMade(agg[11]);
        item.setThreePointsAttempted(agg[12]);
        item.setFreeThrowsMade(agg[13]);
        item.setFreeThrowsAttempted(agg[14]);
        item.setPossessions(BigDecimal.ZERO);
        item.setEfficiency(BigDecimal.ZERO);
        return buildTeamStat(match, item, teamId, opponentTeamId);
    }

    private BasketMatchDataSubmissionTeamStat buildOpponentTeamStatShell(BasketMatch match, Long teamId, Long opponentTeamId,
                                                                         int teamPoints) {
        PlayerMatchDataSubmitReq.TeamStatItem item = new PlayerMatchDataSubmitReq.TeamStatItem();
        item.setTeamId(String.valueOf(teamId));
        item.setPoints(teamPoints);
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
        return buildTeamStat(match, item, teamId, opponentTeamId);
    }

    private void validateSubmitterTeamPlayerAggregate(List<BasketMatchDataSubmissionTeamStat> teamStats,
                                                      Map<Long, int[]> teamAgg, Long submitterTeamId) {
        BasketMatchDataSubmissionTeamStat submitterTeamStat = teamStats.stream()
                .filter(item -> Objects.equals(item.getTeamId(), submitterTeamId))
                .findFirst()
                .orElseThrow(() -> new ServiceException("缺少本队技术统计"));
        int[] agg = teamAgg.get(submitterTeamId);
        if (agg == null) {
            throw new ServiceException("缺少本队球员汇总");
        }
        if (nvl(submitterTeamStat.getPoints()) != agg[0] || nvl(submitterTeamStat.getRebounds()) != agg[1]
                || nvl(submitterTeamStat.getAssists()) != agg[4] || nvl(submitterTeamStat.getSteals()) != agg[5]
                || nvl(submitterTeamStat.getBlocks()) != agg[6] || nvl(submitterTeamStat.getTurnovers()) != agg[7]
                || nvl(submitterTeamStat.getFouls()) != agg[8]) {
            throw new ServiceException("本队统计与球员汇总不一致");
        }
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

    private void validateTeamPlayerAggregate(Map<Long, BasketMatchDataSubmissionTeamStat> teamStatMap,
                                             Map<Long, int[]> teamAgg) {
        for (Map.Entry<Long, BasketMatchDataSubmissionTeamStat> entry : teamStatMap.entrySet()) {
            BasketMatchDataSubmissionTeamStat ts = entry.getValue();
            int[] agg = teamAgg.get(entry.getKey());
            if (agg == null) {
                continue;
            }
            if (nvl(ts.getPoints()) != agg[0] || nvl(ts.getRebounds()) != agg[1]
                    || nvl(ts.getOffensiveRebounds()) != agg[2] || nvl(ts.getDefensiveRebounds()) != agg[3]
                    || nvl(ts.getAssists()) != agg[4] || nvl(ts.getSteals()) != agg[5]
                    || nvl(ts.getBlocks()) != agg[6] || nvl(ts.getTurnovers()) != agg[7]
                    || nvl(ts.getFouls()) != agg[8] || nvl(ts.getFieldGoalsMade()) != agg[9]
                    || nvl(ts.getFieldGoalsAttempted()) != agg[10] || nvl(ts.getThreePointsMade()) != agg[11]
                    || nvl(ts.getThreePointsAttempted()) != agg[12] || nvl(ts.getFreeThrowsMade()) != agg[13]
                    || nvl(ts.getFreeThrowsAttempted()) != agg[14]) {
                throw new ServiceException("球队统计与球员汇总不一致");
            }
        }
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
        if (!hasShootingPointBreakdown(points, fgm, tpm, ftm)) {
            return;
        }
        int theoretical = 2 * (nvl(fgm) - nvl(tpm)) + 3 * nvl(tpm) + nvl(ftm);
        if (nvl(points) != theoretical) {
            throw new ServiceException(label + "得分与投篮数据不一致，理论得分=" + theoretical);
        }
    }

    private boolean hasShootingPointBreakdown(Integer points, Integer fgm, Integer tpm, Integer ftm) {
        int p = nvl(points);
        int fg = nvl(fgm);
        int tp = nvl(tpm);
        int ft = nvl(ftm);
        if (tp > 0 || ft > 0) {
            return true;
        }
        if (fg == 0) {
            return false;
        }
        return fg != p;
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
                                           BasketMatchDataSubmission submission,
                                           Map<String, BasketMatchDataSubmission> submissionByTeamMap,
                                           Map<Long, Long> captainTeamMap,
                                           Long scopedEventTeamId) {
        Long myTeamId = null;
        Long myEventTeamId = scopedEventTeamId;
        if (scopedEventTeamId != null) {
            if (Objects.equals(match.getHomeEventTeamId(), scopedEventTeamId)) {
                myTeamId = match.getHomeTeamId();
            } else if (Objects.equals(match.getAwayEventTeamId(), scopedEventTeamId)) {
                myTeamId = match.getAwayTeamId();
            }
        } else if (myTeamIds.contains(match.getHomeTeamId())) {
            myTeamId = match.getHomeTeamId();
            myEventTeamId = match.getHomeEventTeamId();
        } else if (myTeamIds.contains(match.getAwayTeamId())) {
            myTeamId = match.getAwayTeamId();
            myEventTeamId = match.getAwayEventTeamId();
        }

        String homeName = resolveMatchTeamName(match.getHomeTeamId(), match.getHomeTeamNameSnapshot(), teamNameMap);
        String awayName = resolveMatchTeamName(match.getAwayTeamId(), match.getAwayTeamNameSnapshot(), teamNameMap);

        PlayerMatchTaskResp resp = new PlayerMatchTaskResp();
        resp.setMatchId(String.valueOf(match.getId()));
        resp.setMatchNo(match.getMatchNo());
        resp.setSeasonId(String.valueOf(match.getSeasonId()));
        resp.setStageId(match.getStageId() == null ? null : String.valueOf(match.getStageId()));
        resp.setEventTeamId(scopedEventTeamId == null ? null : String.valueOf(scopedEventTeamId));
        resp.setHomeTeamId(String.valueOf(match.getHomeTeamId()));
        resp.setHomeTeamName(homeName);
        resp.setAwayTeamId(String.valueOf(match.getAwayTeamId()));
        resp.setAwayTeamName(awayName);
        resp.setHomeEventTeamId(match.getHomeEventTeamId() == null ? null : String.valueOf(match.getHomeEventTeamId()));
        resp.setHomeTeamNameSnapshot(match.getHomeTeamNameSnapshot());
        resp.setAwayEventTeamId(match.getAwayEventTeamId() == null ? null : String.valueOf(match.getAwayEventTeamId()));
        resp.setAwayTeamNameSnapshot(match.getAwayTeamNameSnapshot());
        resp.setRoundNo(match.getRoundNo());
        fillScheduleParts(match.getScheduledStartTime(), resp);
        resp.setScheduledStartTime(match.getScheduledStartTime());
        resp.setMatchStatus(match.getMatchStatus());
        resp.setMatchStatusText(resolveEffectiveMatchStatusText(match));
        resp.setPublishStatus(match.getPublishStatus());
        resp.setPublishStatusText(resolvePublishStatusText(match.getPublishStatus()));
        resp.setDataStatus(match.getDataStatus());
        resp.setDataStatusText(resolveDataStatusText(match.getDataStatus(), submission));
        resp.setHomeScore(match.getHomeScore());
        resp.setAwayScore(match.getAwayScore());
        resp.setIsRosterMember(true);
        if (myTeamId != null) {
            resp.setMyTeamId(String.valueOf(myTeamId));
            resp.setMyTeamName(resolveMatchTeamName(myTeamId,
                    Objects.equals(myTeamId, match.getHomeTeamId()) ? match.getHomeTeamNameSnapshot() : match.getAwayTeamNameSnapshot(),
                    teamNameMap));
        }
        if (myEventTeamId != null) {
            resp.setMyEventTeamId(String.valueOf(myEventTeamId));
            resp.setMyTeamNameSnapshot(Objects.equals(myEventTeamId, match.getHomeEventTeamId())
                    ? match.getHomeTeamNameSnapshot() : match.getAwayTeamNameSnapshot());
            Long opponentEventTeamId = Objects.equals(myEventTeamId, match.getHomeEventTeamId())
                    ? match.getAwayEventTeamId() : match.getHomeEventTeamId();
            if (opponentEventTeamId != null) {
                resp.setOpponentEventTeamId(String.valueOf(opponentEventTeamId));
                resp.setOpponentTeamNameSnapshot(Objects.equals(opponentEventTeamId, match.getHomeEventTeamId())
                        ? homeName : awayName);
            }
        }
        boolean isCaptain = myTeamId != null && myTeamId.equals(captainTeamMap.get(match.getId()));
        resp.setIsCaptain(isCaptain);

        if (submission != null) {
            resp.setSubmissionStatus(submission.getSubmissionStatus());
            resp.setSubmissionStatusText(MatchDataSubmissionStatusEnum.fromCode(submission.getSubmissionStatus()).getDescription());
            resp.setMySubmissionStatus(submission.getSubmissionStatus());
            resp.setMySubmissionStatusText(resp.getSubmissionStatusText());
            resp.setSubmissionId(String.valueOf(submission.getId()));
            resp.setSubmissionNo(submission.getSubmissionNo());
            resp.setVersionNo(submission.getVersionNo());
        } else {
            resp.setMySubmissionStatusText("未提交");
        }

        Long opponentTeamIdForSubmission = myTeamId != null
                ? (Objects.equals(myTeamId, match.getHomeTeamId()) ? match.getAwayTeamId() : match.getHomeTeamId())
                : null;
        if (opponentTeamIdForSubmission != null && submissionByTeamMap != null) {
            BasketMatchDataSubmission opponentSubmission = resolveTeamSubmission(
                    submissionByTeamMap, match.getId(), opponentTeamIdForSubmission);
            if (opponentSubmission != null) {
                resp.setOpponentSubmissionStatus(opponentSubmission.getSubmissionStatus());
                resp.setOpponentSubmissionStatusText(
                        MatchDataSubmissionStatusEnum.fromCode(opponentSubmission.getSubmissionStatus()).getDescription());
            } else {
                resp.setOpponentSubmissionStatusText("未提交");
            }
        }

        PlayerMatchDataSubmitSupport.MatchDataSubmitAvailability submitAvailability =
                matchDataSubmitSupport.evaluate(match, isCaptain, myTeamId, submission);
        resp.setCanSubmit(submitAvailability.isCanSubmit());
        resp.setCanResubmit(submitAvailability.isCanResubmit());
        resp.setCanSubmitMatchData(submitAvailability.isCanSubmitMatchData());
        resp.setSubmitDisabledReason(submitAvailability.getSubmitDisabledReason());
        resp.setIsMatchTimePassed(submitAvailability.isMatchTimePassed());
        resp.setIsCurrentUserCaptain(isCaptain);
        boolean confirmed = match.getDataStatus() != null && match.getDataStatus() == BasketPublicConstants.DATA_CONFIRMED;
        resp.setCanViewStatus(submission != null);
        resp.setCanViewData(confirmed || (submission != null
                && Objects.equals(submission.getSubmissionStatus(), MatchDataSubmissionStatusEnum.APPROVED.getCode())));

        PlayerMatchDisputeExceptionSupport.DisputeAvailability disputeAvailability =
                disputeExceptionSupport.evaluateDisputeAvailability(match, isCaptain);
        resp.setCanDispute(Boolean.TRUE.equals(disputeAvailability.getCanDispute()));
        PlayerMatchDisputeExceptionSupport.ExceptionAvailability exceptionAvailability =
                disputeExceptionSupport.evaluateExceptionAvailability(match, isCaptain);
        resp.setCanSubmitException(Boolean.TRUE.equals(exceptionAvailability.getCanSubmitException()));

        resp.setStatusText(buildTaskStatusText(match, submission));
        resp.setActionText(buildTaskActionText(match, submission, resp.getCanSubmit(), resp.getCanResubmit(), resp.getCanViewData()));
        if (submission != null && Objects.equals(submission.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode())) {
            resp.setRejectReason(submission.getRejectReason());
        }
        return resp;
    }

    private void fillSubmitAvailability(PlayerMatchDataSubmissionDetailResp resp, BasketMatch match, Long captainTeamId) {
        BasketMatchDataSubmission latest = findLatestSubmission(match.getId(), captainTeamId);
        PlayerMatchDataSubmitSupport.MatchDataSubmitAvailability availability =
                matchDataSubmitSupport.evaluate(match, true, captainTeamId, latest);
        resp.setCanSubmit(availability.isCanSubmit() || availability.isCanResubmit());
        resp.setCannotSubmitReason(availability.getSubmitDisabledReason());
    }

    private String buildTaskStatusText(BasketMatch match, BasketMatchDataSubmission submission) {
        if (submission != null) {
            return MatchDataSubmissionStatusEnum.fromCode(submission.getSubmissionStatus()).getDescription();
        }
        return resolveDataStatusText(match.getDataStatus());
    }

    private String buildTaskActionText(BasketMatch match, BasketMatchDataSubmission submission,
                                       Boolean canSubmit, Boolean canResubmit, Boolean canViewData) {
        if (Boolean.TRUE.equals(canSubmit)) {
            return "提交数据";
        }
        if (Boolean.TRUE.equals(canResubmit)) {
            return "重新提交";
        }
        if (Boolean.TRUE.equals(canViewData)) {
            return "查看数据";
        }
        if (submission != null) {
            if (Objects.equals(submission.getSubmissionStatus(), MatchDataSubmissionStatusEnum.APPROVED.getCode())) {
                return "查看数据";
            }
            return "查看详情";
        }
        if (!matchDataSubmitSupport.isMatchTimePassed(match)) {
            return "查看详情";
        }
        return "查看详情";
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
        resp.setHomeTeamName(teamNames.get(match.getHomeTeamId()));
        resp.setAwayTeamName(teamNames.get(match.getAwayTeamId()));
        resp.setHomeScore(submission.getHomeScore());
        resp.setAwayScore(submission.getAwayScore());

        boolean isCaptain = isCaptainOfTeam(memberId, submission.getSeasonId(), submission.getSubmitterTeamId());
        boolean rejected = Objects.equals(submission.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode());
        boolean approved = Objects.equals(submission.getSubmissionStatus(), MatchDataSubmissionStatusEnum.APPROVED.getCode());
        resp.setCanResubmit(isCaptain && rejected);
        resp.setCanViewPublicData(approved || Objects.equals(match.getDataStatus(), BasketPublicConstants.DATA_CONFIRMED));
        resp.setStatusText(MatchDataSubmissionStatusEnum.fromCode(submission.getSubmissionStatus()).getDescription());
        fillDisputeExceptionFields(resp, match, memberId);
        return resp;
    }

    private void fillDisputeExceptionFields(PlayerMatchDataStatusResp resp, BasketMatch match, Long memberId) {
        boolean captain = isCaptainOfTeam(memberId, match.getSeasonId(), match.getHomeTeamId())
                || isCaptainOfTeam(memberId, match.getSeasonId(), match.getAwayTeamId());

        PlayerMatchDisputeExceptionSupport.DisputeAvailability disputeAvailability =
                disputeExceptionSupport.evaluateDisputeAvailability(match, captain);
        resp.setCanDispute(disputeAvailability.getCanDispute());
        resp.setCannotDisputeReason(disputeAvailability.getCannotReason());
        resp.setDisputeDeadlineTime(disputeAvailability.getDeadlineTime());
        resp.setDisputeRemainingSeconds(disputeAvailability.getRemainingSeconds());
        if (disputeAvailability.getCurrentDisputeId() != null) {
            resp.setCurrentDisputeId(String.valueOf(disputeAvailability.getCurrentDisputeId()));
        }
        resp.setDisputeStatus(disputeAvailability.getCurrentDisputeStatus());

        PlayerMatchDisputeExceptionSupport.ExceptionAvailability exceptionAvailability =
                disputeExceptionSupport.evaluateExceptionAvailability(match, captain);
        resp.setCanSubmitException(exceptionAvailability.getCanSubmitException());
        resp.setCannotExceptionReason(exceptionAvailability.getCannotReason());
        if (exceptionAvailability.getCurrentExceptionId() != null) {
            resp.setCurrentExceptionId(String.valueOf(exceptionAvailability.getCurrentExceptionId()));
        }
        resp.setExceptionStatus(exceptionAvailability.getCurrentExceptionStatus());
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

    /**
     * 提交校验用出场名单：优先比赛出场名单，缺失时回退赛事阵容快照（与详情页展示一致）。
     */
    private Map<Long, Set<Long>> loadEffectiveRosterPlayerMap(BasketMatch match) {
        Map<Long, Set<Long>> map = loadRosterPlayerMap(match.getId());
        ensureTeamRosterFromEventSnapshot(match, map, match.getHomeTeamId(), match.getHomeEventTeamId());
        ensureTeamRosterFromEventSnapshot(match, map, match.getAwayTeamId(), match.getAwayEventTeamId());
        return map;
    }

    private void ensureTeamRosterFromEventSnapshot(BasketMatch match, Map<Long, Set<Long>> map,
                                                     Long teamId, Long eventTeamId) {
        if (teamId == null) {
            return;
        }
        Set<Long> existing = map.get(teamId);
        if (!CollectionUtils.isEmpty(existing)) {
            return;
        }
        List<EventRosterMemberResp> roster = loadMatchSideRoster(match, eventTeamId, teamId);
        if (CollectionUtils.isEmpty(roster)) {
            return;
        }
        Set<Long> playerIds = new HashSet<>();
        for (EventRosterMemberResp member : roster) {
            if (!StringUtils.hasText(member.getPlayerId())) {
                continue;
            }
            try {
                playerIds.add(Long.parseLong(member.getPlayerId().trim()));
            } catch (NumberFormatException ignored) {
                // skip invalid player id
            }
        }
        if (!playerIds.isEmpty()) {
            map.put(teamId, playerIds);
        }
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

    private void assertNoActiveSubmission(Long matchId, Long submitterTeamId) {
        LambdaQueryWrapper<BasketMatchDataSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchDataSubmission::getMatchId, matchId)
                .eq(BasketMatchDataSubmission::getSubmitterTeamId, submitterTeamId)
                .eq(BasketMatchDataSubmission::getIsDelete, NOT_DELETED)
                .in(BasketMatchDataSubmission::getSubmissionStatus,
                        MatchDataSubmissionStatusEnum.PENDING.getCode(),
                        MatchDataSubmissionStatusEnum.APPROVED.getCode());
        if (submissionDao.selectCount(wrapper) > 0) {
            throw new ServiceException("本队已有提交中的数据，请勿重复提交");
        }
    }

    private BasketMatchDataSubmission findLatestSubmission(Long matchId, Long submitterTeamId) {
        if (matchId == null || submitterTeamId == null) {
            return null;
        }
        LambdaQueryWrapper<BasketMatchDataSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketMatchDataSubmission::getMatchId, matchId)
                .eq(BasketMatchDataSubmission::getSubmitterTeamId, submitterTeamId)
                .eq(BasketMatchDataSubmission::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchDataSubmission::getVersionNo)
                .orderByDesc(BasketMatchDataSubmission::getSubmittedTime)
                .last("LIMIT 1");
        return submissionDao.selectOne(wrapper);
    }

    private Map<String, BasketMatchDataSubmission> loadLatestSubmissionsByTeam(List<Long> matchIds) {
        if (CollectionUtils.isEmpty(matchIds)) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<BasketMatchDataSubmission> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BasketMatchDataSubmission::getMatchId, matchIds)
                .eq(BasketMatchDataSubmission::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchDataSubmission::getVersionNo)
                .orderByDesc(BasketMatchDataSubmission::getSubmittedTime);
        List<BasketMatchDataSubmission> list = submissionDao.selectList(wrapper);
        Map<String, BasketMatchDataSubmission> map = new HashMap<>();
        for (BasketMatchDataSubmission submission : list) {
            map.putIfAbsent(submissionTeamKey(submission.getMatchId(), submission.getSubmitterTeamId()), submission);
        }
        return map;
    }

    private String submissionTeamKey(Long matchId, Long submitterTeamId) {
        return matchId + ":" + submitterTeamId;
    }

    private BasketMatchDataSubmission resolveTeamSubmission(Map<String, BasketMatchDataSubmission> submissionByTeamMap,
                                                            Long matchId, Long submitterTeamId) {
        if (submissionByTeamMap == null || matchId == null || submitterTeamId == null) {
            return null;
        }
        return submissionByTeamMap.get(submissionTeamKey(matchId, submitterTeamId));
    }

    private Long resolveMyTeamIdForMatch(BasketMatch match, List<Long> myTeamIds, Long scopedEventTeamId) {
        if (scopedEventTeamId != null) {
            if (Objects.equals(match.getHomeEventTeamId(), scopedEventTeamId)) {
                return match.getHomeTeamId();
            }
            if (Objects.equals(match.getAwayEventTeamId(), scopedEventTeamId)) {
                return match.getAwayTeamId();
            }
        }
        if (myTeamIds != null && myTeamIds.contains(match.getHomeTeamId())) {
            return match.getHomeTeamId();
        }
        if (myTeamIds != null && myTeamIds.contains(match.getAwayTeamId())) {
            return match.getAwayTeamId();
        }
        return null;
    }

    private Long resolveMemberTeamIdInMatch(Long memberId, BasketMatch match) {
        List<Long> myTeamIds = findActiveTeamIds(memberId, match.getSeasonId());
        return resolveMyTeamIdForMatch(match, myTeamIds, null);
    }

    private BasketMatchDataSubmission findRejectedSubmissionForTeams(Long matchId, List<Long> teamIds) {
        if (CollectionUtils.isEmpty(teamIds)) {
            return null;
        }
        for (Long teamId : teamIds) {
            BasketMatchDataSubmission candidate = findLatestSubmission(matchId, teamId);
            if (candidate != null
                    && Objects.equals(candidate.getSubmissionStatus(), MatchDataSubmissionStatusEnum.REJECTED.getCode())) {
                return candidate;
            }
        }
        return null;
    }

    private Map<Long, String> loadTeamNames(List<BasketMatch> matches) {
        Set<Long> teamIds = new HashSet<>();
        Map<Long, String> map = new HashMap<>();
        for (BasketMatch match : matches) {
            if (match.getHomeTeamId() != null) {
                teamIds.add(match.getHomeTeamId());
                if (StringUtils.hasText(match.getHomeTeamNameSnapshot())) {
                    map.put(match.getHomeTeamId(), match.getHomeTeamNameSnapshot());
                }
            }
            if (match.getAwayTeamId() != null) {
                teamIds.add(match.getAwayTeamId());
                if (StringUtils.hasText(match.getAwayTeamNameSnapshot())) {
                    map.put(match.getAwayTeamId(), match.getAwayTeamNameSnapshot());
                }
            }
        }
        if (teamIds.isEmpty()) {
            return map;
        }
        List<BasketTeam> teams = teamDao.selectBatchIds(teamIds);
        for (BasketTeam team : teams) {
            if (team != null && NOT_DELETED.equals(team.getIsDelete())) {
                map.putIfAbsent(team.getId(), team.getTeamName());
            }
        }
        return map;
    }

    private String resolveMatchTeamName(Long teamId, String snapshot, Map<Long, String> teamNameMap) {
        if (StringUtils.hasText(snapshot)) {
            return snapshot;
        }
        return teamNameMap.get(teamId);
    }

    private void fillScheduleParts(Date scheduledStartTime, PlayerMatchTaskResp resp) {
        if (scheduledStartTime == null) {
            return;
        }
        LocalDateTime dateTime = scheduledStartTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        resp.setMatchDate(dateTime.format(DATE_FORMAT));
        resp.setMatchTime(dateTime.format(TIME_FORMAT));
        resp.setWeekday(formatWeekday(dateTime.getDayOfWeek()));
    }

    private String formatWeekday(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private String resolvePublishStatusText(Integer publishStatus) {
        if (publishStatus == null) {
            return "未知";
        }
        return switch (publishStatus) {
            case 0 -> "草稿";
            case 1 -> "已发布";
            case 2 -> "已下架";
            default -> "未知";
        };
    }

    private BasketTeamSeasonProfile requireEventTeam(Long eventTeamId) {
        BasketTeamSeasonProfile eventTeam = eventTeamDao.selectById(eventTeamId);
        if (eventTeam == null || !NOT_DELETED.equals(eventTeam.getIsDelete())) {
            throw new ServiceException("赛事球队不存在");
        }
        return eventTeam;
    }

    private void requireEventTeamMember(Long memberId, Long eventTeamId, Long seasonId) {
        LambdaQueryWrapper<BasketTournamentSeasonRoster> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId)
                .eq(BasketTournamentSeasonRoster::getSeasonId, seasonId)
                .eq(BasketTournamentSeasonRoster::getMemberId, memberId)
                .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED);
        if (seasonRosterDao.selectCount(wrapper) <= 0) {
            throw new ServiceException("无权查看该赛事球队", 403);
        }
    }

    private Date parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().replace('T', ' ');
        try {
            LocalDateTime dateTime = normalized.length() <= 10
                    ? LocalDate.parse(normalized.substring(0, 10), DATE_FORMAT).atStartOfDay()
                    : LocalDateTime.parse(normalized.length() == 16 ? normalized + ":00" : normalized,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception ex) {
            throw new ServiceException("时间格式无效，请使用 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
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
        page.setPages(0);
        return page;
    }

    private int compareEventTeamMatchesChronological(BasketMatch left, BasketMatch right) {
        Integer leftRound = left.getRoundNo();
        Integer rightRound = right.getRoundNo();
        if (leftRound != null || rightRound != null) {
            if (leftRound == null) {
                return 1;
            }
            if (rightRound == null) {
                return -1;
            }
            int roundCompare = leftRound.compareTo(rightRound);
            if (roundCompare != 0) {
                return roundCompare;
            }
        }
        int timeCompare = compareScheduledTimeAsc(left, right);
        if (timeCompare != 0) {
            return timeCompare;
        }
        return compareMatchTieBreaker(left, right);
    }

    private int compareScheduledTimeAsc(BasketMatch left, BasketMatch right) {
        Date leftTime = left.getScheduledStartTime();
        Date rightTime = right.getScheduledStartTime();
        if (leftTime == null && rightTime == null) {
            return 0;
        }
        if (leftTime == null) {
            return 1;
        }
        if (rightTime == null) {
            return -1;
        }
        return leftTime.compareTo(rightTime);
    }

    private int compareMatchTieBreaker(BasketMatch left, BasketMatch right) {
        Integer leftSort = left.getSortNo();
        Integer rightSort = right.getSortNo();
        if (leftSort != null && rightSort != null && !leftSort.equals(rightSort)) {
            return leftSort.compareTo(rightSort);
        }
        if (leftSort == null && rightSort != null) {
            return 1;
        }
        if (leftSort != null) {
            return -1;
        }
        return Long.compare(left.getId(), right.getId());
    }

    private void enrichMatchDetail(PlayerMatchTaskResp resp, BasketMatch match, BasketTeamSeasonProfile viewerEventTeam,
                                   BasketMatchDataSubmission submission) {
        if (resp == null || match == null) {
            return;
        }
        enrichScheduleSummary(resp, match);

        List<EventRosterMemberResp> homeRoster = loadMatchSideRoster(match, match.getHomeEventTeamId(), match.getHomeTeamId());
        List<EventRosterMemberResp> awayRoster = loadMatchSideRoster(match, match.getAwayEventTeamId(), match.getAwayTeamId());
        enrichRosterAvatars(homeRoster);
        enrichRosterAvatars(awayRoster);
        resp.setHomeRosterList(homeRoster);
        resp.setAwayRosterList(awayRoster);
        resp.setHomeRosterCount(homeRoster.size());
        resp.setAwayRosterCount(awayRoster.size());

        Long myEventTeamId = viewerEventTeam == null ? null : viewerEventTeam.getId();
        if (myEventTeamId != null) {
            if (Objects.equals(myEventTeamId, match.getHomeEventTeamId())) {
                resp.setMyRosterCount(homeRoster.size());
            } else if (Objects.equals(myEventTeamId, match.getAwayEventTeamId())) {
                resp.setMyRosterCount(awayRoster.size());
            }
            if (viewerEventTeam.getRosterLockStatus() != null && viewerEventTeam.getRosterLockStatus() == 1) {
                resp.setRosterStatusText("阵容已锁定");
            } else if (viewerEventTeam.getApprovedTime() != null) {
                resp.setRosterStatusText("正式阵容生效");
            } else {
                resp.setRosterStatusText("阵容生效中");
            }
        }

        resp.setHomeLogoUrl(resolveMatchTeamLogoUrl(match.getHomeTeamLogoSnapshot(), match.getHomeEventTeamId()));
        resp.setAwayLogoUrl(resolveMatchTeamLogoUrl(match.getAwayTeamLogoSnapshot(), match.getAwayEventTeamId()));

        fillTeamRecords(resp, match);

        fillPlayerStatLists(resp, match, homeRoster, awayRoster, submission);
    }

    private void enrichScheduleSummary(PlayerMatchTaskResp resp, BasketMatch match) {
        if (resp == null || match == null) {
            return;
        }
        resp.setPlatformCode(match.getPlatformCode());
        resp.setPlatformName(GameTerminalSupport.formatDisplayName(match.getPlatformCode()));
        if (match.getStageId() != null) {
            BasketTournamentStage stage = stageDao.selectById(match.getStageId());
            if (stage != null && NOT_DELETED.equals(stage.getIsDelete())) {
                resp.setStageName(stage.getStageName());
            }
        }
        if (match.getSeasonId() != null) {
            BasketTournamentSeason season = seasonDao.selectById(match.getSeasonId());
            if (season != null && NOT_DELETED.equals(season.getIsDelete())) {
                BasketTournament tournament = season.getTournamentId() == null
                        ? null : tournamentDao.selectById(season.getTournamentId());
                if (tournament != null && NOT_DELETED.equals(tournament.getIsDelete())) {
                    resp.setEventName(tournament.getTournamentName());
                } else if (StringUtils.hasText(season.getSeasonName())) {
                    resp.setEventName(season.getSeasonName());
                }
                RegistrationRosterRuleSupport.Rule rule = RegistrationRosterRuleSupport.resolve(season, tournament);
                resp.setMatchFormatText(rule.getFormatDisplay());
            }
        }
    }

    private void fillTeamRecords(PlayerMatchTaskResp resp, BasketMatch match) {
        int[] homeRecord = resolveEventTeamRecord(match, match.getHomeEventTeamId());
        int[] awayRecord = resolveEventTeamRecord(match, match.getAwayEventTeamId());
        resp.setHomeWinCount(homeRecord[0]);
        resp.setHomeLossCount(homeRecord[1]);
        resp.setAwayWinCount(awayRecord[0]);
        resp.setAwayLossCount(awayRecord[1]);
        resp.setHomeRecordText(formatRecordText(homeRecord[0], homeRecord[1]));
        resp.setAwayRecordText(formatRecordText(awayRecord[0], awayRecord[1]));
    }

    private int[] resolveEventTeamRecord(BasketMatch match, Long eventTeamId) {
        if (match == null || eventTeamId == null) {
            return zeroRecord();
        }
        BasketTeamSeasonProfile eventTeam = eventTeamDao.selectById(eventTeamId);
        if (eventTeam == null || !NOT_DELETED.equals(eventTeam.getIsDelete())) {
            return zeroRecord();
        }
        Long teamId = eventTeam.getTeamId();
        if (teamId == null) {
            return zeroRecord();
        }

        Long seasonId = match.getSeasonId();
        Long stageId = match.getStageId();
        Long groupId = eventTeam.getGroupId() != null ? eventTeam.getGroupId() : match.getGroupId();
        if (seasonId != null && stageId != null) {
            if (groupId != null && groupId > 0L) {
                int[] groupRecord = loadStandingRecord(seasonId, stageId, groupId, teamId);
                if (groupRecord != null) {
                    return groupRecord;
                }
            }
            int[] stageRecord = loadStandingRecord(seasonId, stageId, STAGE_LEVEL_GROUP_ID, teamId);
            if (stageRecord != null) {
                return stageRecord;
            }
        }
        if (seasonId != null) {
            int[] seasonStatRecord = loadTeamSeasonStatRecord(seasonId, stageId, teamId);
            if (seasonStatRecord != null) {
                return seasonStatRecord;
            }
        }
        return zeroRecord();
    }

    private int[] loadStandingRecord(Long seasonId, Long stageId, Long groupId, Long teamId) {
        BasketStageStanding standing = standingDao.selectOne(new LambdaQueryWrapper<BasketStageStanding>()
                .eq(BasketStageStanding::getSeasonId, seasonId)
                .eq(BasketStageStanding::getStageId, stageId)
                .eq(BasketStageStanding::getGroupId, groupId)
                .eq(BasketStageStanding::getTeamId, teamId)
                .eq(BasketStageStanding::getIsDelete, NOT_DELETED)
                .last("LIMIT 1"));
        if (standing == null) {
            return null;
        }
        return new int[]{safeCount(standing.getWins()), safeCount(standing.getLosses())};
    }

    private int[] loadTeamSeasonStatRecord(Long seasonId, Long stageId, Long teamId) {
        LambdaQueryWrapper<BasketTeamSeasonStat> wrapper = new LambdaQueryWrapper<BasketTeamSeasonStat>()
                .eq(BasketTeamSeasonStat::getSeasonId, seasonId)
                .eq(BasketTeamSeasonStat::getTeamId, teamId)
                .eq(BasketTeamSeasonStat::getIsDelete, NOT_DELETED);
        if (stageId != null) {
            wrapper.eq(BasketTeamSeasonStat::getStageId, stageId);
        } else {
            wrapper.isNull(BasketTeamSeasonStat::getStageId);
        }
        BasketTeamSeasonStat stat = teamSeasonStatDao.selectOne(wrapper.last("LIMIT 1"));
        if (stat == null && stageId != null) {
            stat = teamSeasonStatDao.selectOne(new LambdaQueryWrapper<BasketTeamSeasonStat>()
                    .eq(BasketTeamSeasonStat::getSeasonId, seasonId)
                    .eq(BasketTeamSeasonStat::getTeamId, teamId)
                    .eq(BasketTeamSeasonStat::getIsDelete, NOT_DELETED)
                    .isNull(BasketTeamSeasonStat::getStageId)
                    .last("LIMIT 1"));
        }
        if (stat == null) {
            return null;
        }
        return new int[]{safeCount(stat.getWins()), safeCount(stat.getLosses())};
    }

    private int[] zeroRecord() {
        return new int[]{0, 0};
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private String formatRecordText(int wins, int losses) {
        return wins + "胜" + losses + "负";
    }

    private void fillPlayerStatLists(PlayerMatchTaskResp resp, BasketMatch match,
                                     List<EventRosterMemberResp> homeRoster,
                                     List<EventRosterMemberResp> awayRoster,
                                     BasketMatchDataSubmission submission) {
        boolean confirmed = match.getDataStatus() != null
                && match.getDataStatus() == BasketPublicConstants.DATA_CONFIRMED;
        if (confirmed) {
            List<BasketMatchPlayerStat> officialStats = matchPlayerStatDao.selectList(
                    new LambdaQueryWrapper<BasketMatchPlayerStat>()
                            .eq(BasketMatchPlayerStat::getMatchId, match.getId())
                            .eq(BasketMatchPlayerStat::getIsDelete, NOT_DELETED)
                            .orderByAsc(BasketMatchPlayerStat::getTeamId)
                            .orderByAsc(BasketMatchPlayerStat::getId));
            if (!CollectionUtils.isEmpty(officialStats)) {
                resp.setHomePlayerStatList(buildOfficialPlayerStats(officialStats, match.getHomeTeamId(), homeRoster,
                        match.getHomeTeamNameSnapshot(), match.getHomeEventTeamId(), match.getPlatformCode()));
                resp.setAwayPlayerStatList(buildOfficialPlayerStats(officialStats, match.getAwayTeamId(), awayRoster,
                        match.getAwayTeamNameSnapshot(), match.getAwayEventTeamId(), match.getPlatformCode()));
                return;
            }
        }

        if (submission != null) {
            List<BasketMatchDataSubmissionPlayerStat> submissionStats = submissionPlayerStatDao.selectList(
                    new LambdaQueryWrapper<BasketMatchDataSubmissionPlayerStat>()
                            .eq(BasketMatchDataSubmissionPlayerStat::getSubmissionId, submission.getId())
                            .eq(BasketMatchDataSubmissionPlayerStat::getIsDelete, NOT_DELETED)
                            .orderByAsc(BasketMatchDataSubmissionPlayerStat::getSortNo)
                            .orderByAsc(BasketMatchDataSubmissionPlayerStat::getId));
            if (!CollectionUtils.isEmpty(submissionStats)) {
                resp.setHomePlayerStatList(buildSubmissionPlayerStats(submissionStats, match.getHomeTeamId(), homeRoster,
                        match.getHomeTeamNameSnapshot(), match.getHomeEventTeamId(), match.getPlatformCode()));
                resp.setAwayPlayerStatList(buildSubmissionPlayerStats(submissionStats, match.getAwayTeamId(), awayRoster,
                        match.getAwayTeamNameSnapshot(), match.getAwayEventTeamId(), match.getPlatformCode()));
                return;
            }
        }

        resp.setHomePlayerStatList(buildEmptyPlayerStats(homeRoster, match.getHomeTeamNameSnapshot(),
                match.getHomeEventTeamId(), match.getHomeTeamId(), match.getPlatformCode()));
        resp.setAwayPlayerStatList(buildEmptyPlayerStats(awayRoster, match.getAwayTeamNameSnapshot(),
                match.getAwayEventTeamId(), match.getAwayTeamId(), match.getPlatformCode()));
    }

    private List<MatchDetailPlayerStatResp> buildOfficialPlayerStats(List<BasketMatchPlayerStat> stats, Long teamId,
                                                                     List<EventRosterMemberResp> roster,
                                                                     String teamNameSnapshot, Long eventTeamId,
                                                                     String platformCode) {
        Map<String, EventRosterMemberResp> rosterMap = indexRosterByPlayerId(roster);
        List<MatchDetailPlayerStatResp> result = new ArrayList<>();
        for (BasketMatchPlayerStat stat : stats) {
            if (teamId != null && !Objects.equals(stat.getTeamId(), teamId)) {
                continue;
            }
            EventRosterMemberResp member = rosterMap.get(String.valueOf(stat.getPlayerId()));
            result.add(toOfficialPlayerStatResp(stat, member, teamNameSnapshot, eventTeamId, platformCode));
        }
        if (result.isEmpty() && !CollectionUtils.isEmpty(roster)) {
            return buildEmptyPlayerStats(roster, teamNameSnapshot, eventTeamId, teamId, platformCode);
        }
        return result;
    }

    private List<MatchDetailPlayerStatResp> buildSubmissionPlayerStats(List<BasketMatchDataSubmissionPlayerStat> stats,
                                                                        Long teamId,
                                                                        List<EventRosterMemberResp> roster,
                                                                        String teamNameSnapshot, Long eventTeamId,
                                                                        String platformCode) {
        Map<String, EventRosterMemberResp> rosterMap = indexRosterByPlayerId(roster);
        List<MatchDetailPlayerStatResp> result = new ArrayList<>();
        for (BasketMatchDataSubmissionPlayerStat stat : stats) {
            if (teamId != null && !Objects.equals(stat.getTeamId(), teamId)) {
                continue;
            }
            EventRosterMemberResp member = rosterMap.get(String.valueOf(stat.getPlayerId()));
            result.add(toSubmissionPlayerStatResp(stat, member, teamNameSnapshot, eventTeamId, platformCode));
        }
        if (result.isEmpty() && !CollectionUtils.isEmpty(roster)) {
            return buildEmptyPlayerStats(roster, teamNameSnapshot, eventTeamId, teamId, platformCode);
        }
        return result;
    }

    private List<MatchDetailPlayerStatResp> buildEmptyPlayerStats(List<EventRosterMemberResp> roster,
                                                                  String teamNameSnapshot, Long eventTeamId,
                                                                  Long teamId, String platformCode) {
        if (CollectionUtils.isEmpty(roster)) {
            return Collections.emptyList();
        }
        List<MatchDetailPlayerStatResp> result = new ArrayList<>(roster.size());
        for (EventRosterMemberResp member : roster) {
            MatchDetailPlayerStatResp item = new MatchDetailPlayerStatResp();
            item.setPlayerId(member.getPlayerId());
            item.setPlayerNo(member.getPlayerNo());
            item.setPlayerName(member.getNickname());
            item.setAvatarUrl(member.getAvatarUrl());
            item.setRosterId(member.getRosterId());
            item.setTeamNameSnapshot(teamNameSnapshot);
            item.setEventTeamId(eventTeamId == null ? null : String.valueOf(eventTeamId));
            item.setTeamId(teamId == null ? null : String.valueOf(teamId));
            item.setPosition(member.getPosition());
            item.setGameId(resolveDisplayGameId(member, platformCode));
            item.setStatusText(member.getRosterStatusText());
            result.add(item);
        }
        return result;
    }

    private MatchDetailPlayerStatResp toOfficialPlayerStatResp(BasketMatchPlayerStat stat,
                                                               EventRosterMemberResp member,
                                                               String teamNameSnapshot, Long eventTeamId,
                                                               String platformCode) {
        MatchDetailPlayerStatResp item = new MatchDetailPlayerStatResp();
        item.setPlayerId(stat.getPlayerId() == null ? null : String.valueOf(stat.getPlayerId()));
        item.setPlayerNo(member == null ? null : member.getPlayerNo());
        item.setPlayerName(member == null ? null : member.getNickname());
        item.setAvatarUrl(member == null ? null : member.getAvatarUrl());
        item.setTeamNameSnapshot(teamNameSnapshot);
        item.setEventTeamId(eventTeamId == null ? null : String.valueOf(eventTeamId));
        item.setPosition(member == null ? null : member.getPosition());
        item.setGameId(member == null ? null : resolveDisplayGameId(member, platformCode));
        item.setPoints(stat.getPoints());
        item.setRebounds(stat.getRebounds());
        item.setAssists(stat.getAssists());
        item.setSteals(stat.getSteals());
        item.setBlocks(stat.getBlocks());
        item.setTurnovers(stat.getTurnovers());
        item.setFouls(stat.getFouls());
        item.setStatusText("已确认");
        return item;
    }

    private MatchDetailPlayerStatResp toSubmissionPlayerStatResp(BasketMatchDataSubmissionPlayerStat stat,
                                                                 EventRosterMemberResp member,
                                                                 String teamNameSnapshot, Long eventTeamId,
                                                                 String platformCode) {
        MatchDetailPlayerStatResp item = new MatchDetailPlayerStatResp();
        item.setPlayerId(stat.getPlayerId() == null ? null : String.valueOf(stat.getPlayerId()));
        item.setPlayerNo(member == null ? null : member.getPlayerNo());
        item.setPlayerName(member == null ? null : member.getNickname());
        item.setAvatarUrl(member == null ? null : member.getAvatarUrl());
        item.setTeamNameSnapshot(teamNameSnapshot);
        item.setEventTeamId(eventTeamId == null ? null : String.valueOf(eventTeamId));
        item.setPosition(member == null ? null : member.getPosition());
        item.setGameId(member == null ? null : resolveDisplayGameId(member, platformCode));
        item.setPoints(stat.getPoints());
        item.setRebounds(stat.getRebounds());
        item.setAssists(stat.getAssists());
        item.setSteals(stat.getSteals());
        item.setBlocks(stat.getBlocks());
        item.setTurnovers(stat.getTurnovers());
        item.setFouls(stat.getFouls());
        item.setStatusText("待审核");
        return item;
    }

    private Map<String, EventRosterMemberResp> indexRosterByPlayerId(List<EventRosterMemberResp> roster) {
        if (CollectionUtils.isEmpty(roster)) {
            return Collections.emptyMap();
        }
        Map<String, EventRosterMemberResp> map = new HashMap<>();
        for (EventRosterMemberResp member : roster) {
            if (StringUtils.hasText(member.getPlayerId())) {
                map.put(member.getPlayerId(), member);
            }
        }
        return map;
    }

    private String resolveDisplayGameId(EventRosterMemberResp member, String platformCode) {
        if (member == null) {
            return null;
        }
        String gameId = member.getGameId();
        if (!StringUtils.hasText(gameId) || "-".equals(gameId) || "未绑定".equals(gameId)) {
            return null;
        }
        String platform = member.getPlatform();
        if (StringUtils.hasText(platformCode) && StringUtils.hasText(platform)
                && !platform.equalsIgnoreCase(platformCode)) {
            return gameId;
        }
        if (GAME_ID_PLATFORM_PREFIX.matcher(gameId).find()) {
            return gameId;
        }
        if (StringUtils.hasText(platform) && !"-".equals(platform)) {
            return platform + ": " + gameId;
        }
        return gameId;
    }

    private List<EventRosterMemberResp> loadMatchSideRoster(BasketMatch match, Long eventTeamId, Long teamId) {
        if (eventTeamId != null) {
            List<EventRosterMemberResp> eventRoster = loadEventTeamRoster(eventTeamId);
            if (!eventRoster.isEmpty()) {
                return eventRoster;
            }
        }
        if (match.getId() != null && teamId != null) {
            List<EventRosterMemberResp> matchRoster = loadMatchRosterEntries(match, teamId, eventTeamId);
            if (!matchRoster.isEmpty()) {
                return matchRoster;
            }
        }
        if (match.getSeasonId() != null && teamId != null) {
            return loadSeasonRosterByTeam(match.getSeasonId(), teamId);
        }
        return Collections.emptyList();
    }

    private List<EventRosterMemberResp> loadMatchRosterEntries(BasketMatch match, Long teamId, Long eventTeamId) {
        List<BasketMatchRoster> matchRosters = matchRosterDao.selectList(new LambdaQueryWrapper<BasketMatchRoster>()
                .eq(BasketMatchRoster::getMatchId, match.getId())
                .eq(BasketMatchRoster::getTeamId, teamId)
                .eq(BasketMatchRoster::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketMatchRoster::getCaptainFlag)
                .orderByAsc(BasketMatchRoster::getSortNo)
                .orderByAsc(BasketMatchRoster::getCreateTime));
        if (matchRosters.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, BasketTournamentSeasonRoster> snapshotMap = loadSeasonRosterSnapshotMap(eventTeamId, match.getSeasonId(), teamId);
        Set<Long> playerIds = matchRosters.stream()
                .map(BasketMatchRoster::getPlayerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BasketPlayerProfile> playerMap = loadPlayerProfileMap(playerIds);
        List<EventRosterMemberResp> result = new ArrayList<>();
        for (BasketMatchRoster roster : matchRosters) {
            BasketTournamentSeasonRoster snapshot = snapshotMap.get(roster.getPlayerId());
            if (snapshot != null) {
                result.add(toEventRosterMemberResp(snapshot, playerMap.get(roster.getPlayerId())));
            } else {
                result.add(toMatchRosterMemberResp(roster, playerMap.get(roster.getPlayerId())));
            }
        }
        return result;
    }

    private Map<Long, BasketTournamentSeasonRoster> loadSeasonRosterSnapshotMap(Long eventTeamId, Long seasonId, Long teamId) {
        LambdaQueryWrapper<BasketTournamentSeasonRoster> wrapper = new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED);
        if (eventTeamId != null) {
            wrapper.eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId);
        } else if (seasonId != null && teamId != null) {
            wrapper.eq(BasketTournamentSeasonRoster::getSeasonId, seasonId)
                    .eq(BasketTournamentSeasonRoster::getTeamId, teamId);
        } else {
            return Collections.emptyMap();
        }
        List<BasketTournamentSeasonRoster> rosterList = seasonRosterDao.selectList(wrapper);
        Map<Long, BasketTournamentSeasonRoster> map = new HashMap<>();
        for (BasketTournamentSeasonRoster roster : rosterList) {
            if (roster.getPlayerId() != null) {
                map.putIfAbsent(roster.getPlayerId(), roster);
            }
        }
        return map;
    }

    private List<EventRosterMemberResp> loadSeasonRosterByTeam(Long seasonId, Long teamId) {
        List<BasketTournamentSeasonRoster> rosterList = seasonRosterDao.selectList(
                new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                        .eq(BasketTournamentSeasonRoster::getSeasonId, seasonId)
                        .eq(BasketTournamentSeasonRoster::getTeamId, teamId)
                        .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                        .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED)
                        .orderByDesc(BasketTournamentSeasonRoster::getIsCaptain)
                        .orderByAsc(BasketTournamentSeasonRoster::getCreateTime));
        if (rosterList.isEmpty()) {
            rosterList = seasonRosterDao.selectList(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                    .eq(BasketTournamentSeasonRoster::getSeasonId, seasonId)
                    .eq(BasketTournamentSeasonRoster::getTeamId, teamId)
                    .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED)
                    .orderByDesc(BasketTournamentSeasonRoster::getIsCaptain)
                    .orderByAsc(BasketTournamentSeasonRoster::getCreateTime));
        }
        return convertSeasonRosterList(rosterList);
    }

    private List<EventRosterMemberResp> loadEventTeamRoster(Long eventTeamId) {
        List<BasketTournamentSeasonRoster> rosterList = seasonRosterDao.selectList(
                new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                        .eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId)
                        .eq(BasketTournamentSeasonRoster::getRosterStatus, RosterStatusEnum.ACTIVE.getCode())
                        .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED)
                        .orderByDesc(BasketTournamentSeasonRoster::getIsCaptain)
                        .orderByAsc(BasketTournamentSeasonRoster::getCreateTime));
        if (rosterList.isEmpty()) {
            rosterList = seasonRosterDao.selectList(new LambdaQueryWrapper<BasketTournamentSeasonRoster>()
                    .eq(BasketTournamentSeasonRoster::getEventTeamId, eventTeamId)
                    .eq(BasketTournamentSeasonRoster::getIsDelete, NOT_DELETED)
                    .orderByDesc(BasketTournamentSeasonRoster::getIsCaptain)
                    .orderByAsc(BasketTournamentSeasonRoster::getCreateTime));
        }
        return convertSeasonRosterList(rosterList);
    }

    private List<EventRosterMemberResp> convertSeasonRosterList(List<BasketTournamentSeasonRoster> rosterList) {
        if (CollectionUtils.isEmpty(rosterList)) {
            return Collections.emptyList();
        }
        Set<Long> playerIds = rosterList.stream()
                .map(BasketTournamentSeasonRoster::getPlayerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, BasketPlayerProfile> playerMap = loadPlayerProfileMap(playerIds);
        List<EventRosterMemberResp> result = new ArrayList<>();
        for (BasketTournamentSeasonRoster roster : rosterList) {
            result.add(toEventRosterMemberResp(roster, playerMap.get(roster.getPlayerId())));
        }
        return result;
    }

    private Map<Long, BasketPlayerProfile> loadPlayerProfileMap(Set<Long> playerIds) {
        Map<Long, BasketPlayerProfile> playerMap = new HashMap<>();
        if (playerIds.isEmpty()) {
            return playerMap;
        }
        for (BasketPlayerProfile profile : playerProfileDao.selectBatchIds(playerIds)) {
            if (profile != null && NOT_DELETED.equals(profile.getIsDelete())) {
                playerMap.put(profile.getId(), profile);
            }
        }
        return playerMap;
    }

    private EventRosterMemberResp toMatchRosterMemberResp(BasketMatchRoster roster, BasketPlayerProfile profile) {
        EventRosterMemberResp resp = new EventRosterMemberResp();
        resp.setRosterId(toStringId(roster.getId()));
        resp.setPlayerId(toStringId(roster.getPlayerId()));
        resp.setPlayerNo(profile == null ? null : profile.getPlayerNo());
        resp.setNickname(profile == null ? null : firstNonBlank(profile.getGameNickname(), profile.getPlayerNo()));
        resp.setPlatform("-");
        resp.setGameId("-");
        resp.setPosition(roster.getPositionCode());
        resp.setRosterRole(roster.getRosterRole());
        RosterRoleEnum role = StringUtils.hasText(roster.getRosterRole())
                ? safeRosterRole(roster.getRosterRole()) : RosterRoleEnum.PLAYER;
        resp.setRosterRoleText(roster.getCaptainFlag() != null && roster.getCaptainFlag() == 1
                ? "赛事队长" : role.getDescription());
        resp.setIsCaptain(roster.getCaptainFlag());
        resp.setIsStarting(roster.getIsStarting());
        resp.setRosterStatus(RosterStatusEnum.ACTIVE.getCode());
        resp.setRosterStatusText(RosterStatusEnum.ACTIVE.getDescription());
        return resp;
    }

    private void enrichRosterAvatars(List<EventRosterMemberResp> roster) {
        if (CollectionUtils.isEmpty(roster)) {
            return;
        }
        Set<Long> playerIds = roster.stream()
                .map(member -> parseLongIdQuietly(member.getPlayerId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (playerIds.isEmpty()) {
            return;
        }
        Map<Long, BasketPlayerProfile> playerMap = loadPlayerProfileMap(playerIds);
        Map<Long, String> avatarMap = publicSupport.loadPlayerAvatarUrlMap(playerMap.values());
        for (EventRosterMemberResp member : roster) {
            Long playerId = parseLongIdQuietly(member.getPlayerId());
            String avatarUrl = playerId == null ? null : avatarMap.get(playerId);
            member.setAvatarUrl(MemberAvatarSupport.resolve(avatarUrl));
        }
    }

    private Long parseLongIdQuietly(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveMatchTeamLogoUrl(String logoSnapshot, Long eventTeamId) {
        if (StringUtils.hasText(logoSnapshot)) {
            return logoSnapshot.trim();
        }
        if (eventTeamId == null) {
            return null;
        }
        BasketTeamSeasonProfile eventTeam = eventTeamDao.selectById(eventTeamId);
        if (eventTeam == null || !NOT_DELETED.equals(eventTeam.getIsDelete())) {
            return null;
        }
        return resolveEventTeamLogoUrl(eventTeam.getLogoFileId());
    }

    private String resolveEventTeamLogoUrl(Long logoFileId) {
        if (logoFileId == null) {
            return null;
        }
        try {
            AccessibleFile file = accountFileService.getAccessibleFile(logoFileId);
            if (file != null && file.getAsset() != null && StringUtils.hasText(file.getAsset().getAccessUrl())) {
                return file.getAsset().getAccessUrl();
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private EventRosterMemberResp toEventRosterMemberResp(BasketTournamentSeasonRoster roster, BasketPlayerProfile profile) {
        RosterStatusEnum rosterStatus = safeRosterStatus(roster.getRosterStatus());
        RosterRoleEnum role = safeRosterRole(roster.getRosterRole());
        EventRosterMemberResp resp = new EventRosterMemberResp();
        resp.setRosterId(toStringId(roster.getId()));
        resp.setPlayerId(toStringId(roster.getPlayerId()));
        resp.setPlayerNo(profile == null ? null : profile.getPlayerNo());
        resp.setNickname(firstNonBlank(roster.getPlayerNicknameSnapshot(),
                profile == null ? null : profile.getGameNickname()));
        resp.setPlatform(StringUtils.hasText(roster.getPlatformSnapshot()) ? roster.getPlatformSnapshot() : "-");
        resp.setGameId(StringUtils.hasText(roster.getGameIdDisplaySnapshot()) ? roster.getGameIdDisplaySnapshot() : "-");
        resp.setPosition(roster.getPositionSnapshot());
        resp.setRosterRole(role.getCode());
        resp.setRosterRoleText(roster.getIsCaptain() != null && roster.getIsCaptain() == 1
                ? "赛事队长" : role.getDescription());
        resp.setIsCaptain(roster.getIsCaptain());
        resp.setIsStarting(roster.getIsStarting());
        resp.setRosterStatus(rosterStatus.getCode());
        resp.setRosterStatusText(resolveRosterDisplayStatus(roster, rosterStatus));
        resp.setSourceType(roster.getSourceType());
        resp.setEffectiveTime(roster.getEffectiveTime());
        return resp;
    }

    private RosterStatusEnum safeRosterStatus(Integer code) {
        if (code == null) {
            return RosterStatusEnum.ACTIVE;
        }
        try {
            return RosterStatusEnum.fromCode(code);
        } catch (Exception ex) {
            return RosterStatusEnum.ACTIVE;
        }
    }

    private RosterRoleEnum safeRosterRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return RosterRoleEnum.PLAYER;
        }
        try {
            return RosterRoleEnum.fromCode(roleCode);
        } catch (Exception ex) {
            return RosterRoleEnum.PLAYER;
        }
    }

    private String resolveRosterDisplayStatus(BasketTournamentSeasonRoster roster, RosterStatusEnum rosterStatus) {
        if (roster.getLockStatus() != null && roster.getLockStatus() == 1) {
            return "已锁定";
        }
        if (rosterStatus == RosterStatusEnum.ACTIVE) {
            return "生效中";
        }
        return rosterStatus.getDescription();
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        return second;
    }

    private String toStringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private void ensureMatchFinishedIfScheduled(BasketMatch match) {
        if (match == null || match.getId() == null) {
            return;
        }
        if (!matchDataSubmitSupport.isMatchTimePassed(match)) {
            return;
        }
        if (matchDataSubmitSupport.isMatchCancelled(match) || matchDataSubmitSupport.isMatchPostponed(match)) {
            return;
        }
        if (Objects.equals(match.getMatchStatus(), BasketPublicConstants.MATCH_FINISHED)) {
            return;
        }
        matchDao.finishScheduledMatchIfTimePassed(
                match.getId(),
                BasketPublicConstants.MATCH_SCHEDULED,
                BasketPublicConstants.MATCH_FINISHED,
                BasketPublicConstants.PUBLISH_PUBLISHED,
                NOT_DELETED);
    }

    private String resolveEffectiveMatchStatusText(BasketMatch match) {
        if (matchDataSubmitSupport.shouldDisplayAsFinished(match)) {
            return "已结束";
        }
        return resolveMatchStatusText(match.getMatchStatus());
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
        return resolveDataStatusText(status, null);
    }

    private String resolveDataStatusText(Integer status, BasketMatchDataSubmission submission) {
        if (submission != null) {
            MatchDataSubmissionStatusEnum submissionStatus = MatchDataSubmissionStatusEnum.fromCode(submission.getSubmissionStatus());
            return switch (submissionStatus) {
                case PENDING -> "审核中";
                case APPROVED -> "已通过";
                case REJECTED -> "已驳回";
                default -> submissionStatus.getDescription();
            };
        }
        return "未提交";
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
