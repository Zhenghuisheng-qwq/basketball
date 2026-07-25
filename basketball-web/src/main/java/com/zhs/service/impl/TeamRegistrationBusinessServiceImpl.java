package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.*;
import com.zhs.enums.*;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketMessageTask;
import com.zhs.model.BasketMemberAccount;
import com.zhs.model.BasketCompetitionSeason;
import com.zhs.model.BasketDisciplineRecord;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentRegistration;
import com.zhs.model.BasketTournamentRegistrationAudit;
import com.zhs.model.BasketTournamentRegistrationPlayer;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.request.team.TeamRegistrationDraftReq;
import com.zhs.request.team.TeamRegistrationRosterItemReq;
import com.zhs.request.team.TeamRegistrationRosterReq;
import com.zhs.request.team.TeamRegistrationUpdateReq;
import com.zhs.request.team.TeamRegistrationWithdrawReq;
import com.zhs.request.team.TeamRegistrationRemindReq;
import com.zhs.response.account.MyEventsContextResp;
import com.zhs.response.team.*;
import com.zhs.service.IBusinessMessageService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.ITeamRegistrationBusinessService;
import com.zhs.util.DataMaskUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeamRegistrationBusinessServiceImpl implements ITeamRegistrationBusinessService {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;
    private static final int IS_CAPTAIN = 1;
    private static final int IS_STARTING = 1;
    private static final int REQUIRED_STARTING_COUNT = 5;
    private static final int DELETED = 1;
    private static final int ENABLED = 1;
    private static final String BIZ_TYPE_TEAM_REGISTRATION = "TEAM_REGISTRATION";
    private static final String BIZ_TYPE_TEAM_REGISTRATION_REMIND = "TEAM_REGISTRATION_REMIND";
    private static final long REMIND_COOLDOWN_MS = 24L * 60 * 60 * 1000;

    @Resource
    private BasketMessageTaskDao messageTaskDao;

    @Resource
    private BasketTournamentSeasonDao seasonDao;
    @Resource
    private BasketTournamentDao tournamentDao;
    @Resource
    private BasketTournamentRegistrationDao registrationDao;
    @Resource
    private BasketTournamentRegistrationPlayerDao registrationPlayerDao;
    @Resource
    private BasketTournamentRegistrationAuditDao registrationAuditDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketCompetitionSeasonDao competitionSeasonDao;
    @Resource
    private BasketDisciplineRecordDao disciplineRecordDao;
    @Resource
    private BasketMemberAccountDao memberAccountDao;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private IBusinessMessageService businessMessageService;

    @Override
    public AvailableSeasonPageResp pageAvailableSeasons(String teamId, Integer currentPage, Integer pageSize) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);
        Date now = new Date();

        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            AvailableSeasonPageResp emptyPage = buildAvailablePage(Collections.emptyList(), currentPage, pageSize);
            emptyPage.setEmptyReason("当前球队尚未认证，认证通过后可报名赛事");
            return emptyPage;
        }

        List<BasketTournamentSeason> seasons = seasonDao.selectList(new LambdaQueryWrapper<BasketTournamentSeason>()
                .eq(BasketTournamentSeason::getIsDelete, NOT_DELETED)
                .and(w -> w.eq(BasketTournamentSeason::getAllowTeamRegistration, ENABLED)
                        .or().isNull(BasketTournamentSeason::getAllowTeamRegistration))
                .notIn(BasketTournamentSeason::getSeasonStatus,
                        TournamentSeasonStatusEnum.FINISHED.getCode(),
                        TournamentSeasonStatusEnum.CLOSED.getCode(),
                        TournamentSeasonStatusEnum.CANCELLED.getCode())
                .orderByDesc(BasketTournamentSeason::getRegistrationStartTime));

        Map<Long, BasketTournament> tournamentMap = loadTournamentMap(seasons);
        Map<Long, BasketCompetitionSeason> competitionSeasonMap = loadCompetitionSeasonMap(tournamentMap.values());
        Map<Long, BasketTournamentRegistration> existingMap = loadExistingRegistrationMap(teamPk, seasons);
        Map<Long, Long> seasonRegistrationCountMap = loadSeasonRegistrationCountMap(seasons);

        List<AvailableSeasonItemResp> registerableItems = new ArrayList<>();
        int inWindowCount = 0;
        int notStartedCount = 0;
        int terminalMismatchCount = 0;
        int alreadyRegisteredCount = 0;

        for (BasketTournamentSeason season : seasons) {
            BasketTournament tournament = tournamentMap.get(season.getTournamentId());
            if (tournament == null || !Objects.equals(TournamentStatusEnum.ACTIVE.getCode(), tournament.getTournamentStatus())) {
                continue;
            }
            if (!isCompetitionSeasonValid(tournament, competitionSeasonMap)) {
                continue;
            }

            String registrationStatus = resolveRegistrationStatus(season, tournament, now);
            if ("OPEN".equals(registrationStatus)) {
                inWindowCount++;
            } else if ("NOT_STARTED".equals(registrationStatus)) {
                notStartedCount++;
            }

            BasketTournamentRegistration existing = existingMap.get(season.getId());
            boolean alreadyRegistered = existing != null && isBlockingRegistration(existing.getRegistrationStatus());
            if (alreadyRegistered) {
                alreadyRegisteredCount++;
                continue;
            }

            String terminalMismatchReason = resolveTerminalMismatchReason(team, tournament);
            if (terminalMismatchReason != null) {
                if ("OPEN".equals(registrationStatus)) {
                    terminalMismatchCount++;
                }
                continue;
            }

            String quotaReason = resolveQuotaExceededReason(season, seasonRegistrationCountMap);
            if (quotaReason != null) {
                continue;
            }

            if (!"OPEN".equals(registrationStatus)) {
                continue;
            }

            AvailableSeasonItemResp resp = buildAvailableSeasonItem(
                    season, tournament, team, existing, registrationStatus, competitionSeasonMap);
            resp.setCanRegister(true);
            registerableItems.add(resp);
        }

        AvailableSeasonPageResp page = buildAvailablePage(registerableItems, currentPage, pageSize);
        if (registerableItems.isEmpty()) {
            page.setEmptyReason(resolveEmptyReason(team, inWindowCount, notStartedCount, terminalMismatchCount, alreadyRegisteredCount, seasons.size()));
        }
        return page;
    }

    @Override
    public MyEventsContextResp getMyEventsContext() {
        Long memberId = UserContext.getRequiredUserId();
        MyEventsContextResp resp = new MyEventsContextResp();
        resp.setHasTeam(false);
        resp.setTeamRole("NONE");

        BasketTeamMember membership = teamMemberDao.selectOne(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamMember::getCreateTime)
                .last("limit 1"));
        if (membership == null) {
            return resp;
        }

        BasketTeam team = teamDao.selectById(membership.getTeamId());
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            return resp;
        }

        resp.setHasTeam(true);
        resp.setTeamId(toStringId(team.getId()));
        resp.setTeamName(team.getTeamName());
        resp.setTeamCertificationStatus(team.getAuditStatus());
        resp.setGameTerminal(team.getGameTerminal());
        resp.setTeamRole(memberId.equals(team.getCaptainMemberId()) ? "CAPTAIN" : "MEMBER");
        return resp;
    }

    @Override
    public AvailableSeasonPageResp pageMemberAvailableSeasons(String teamId, Integer currentPage, Integer pageSize) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = requireTeamForMemberView(memberId, teamPk);
        Date now = new Date();

        List<BasketTournamentSeason> seasons = seasonDao.selectList(new LambdaQueryWrapper<BasketTournamentSeason>()
                .eq(BasketTournamentSeason::getIsDelete, NOT_DELETED)
                .and(w -> w.eq(BasketTournamentSeason::getAllowTeamRegistration, ENABLED)
                        .or().isNull(BasketTournamentSeason::getAllowTeamRegistration))
                .notIn(BasketTournamentSeason::getSeasonStatus,
                        TournamentSeasonStatusEnum.FINISHED.getCode(),
                        TournamentSeasonStatusEnum.CLOSED.getCode(),
                        TournamentSeasonStatusEnum.CANCELLED.getCode())
                .orderByDesc(BasketTournamentSeason::getRegistrationStartTime));

        Map<Long, BasketTournament> tournamentMap = loadTournamentMap(seasons);
        Map<Long, BasketCompetitionSeason> competitionSeasonMap = loadCompetitionSeasonMap(tournamentMap.values());
        Map<Long, BasketTournamentRegistration> existingMap = loadExistingRegistrationMap(teamPk, seasons);

        List<AvailableSeasonItemResp> items = new ArrayList<>();
        for (BasketTournamentSeason season : seasons) {
            BasketTournament tournament = tournamentMap.get(season.getTournamentId());
            if (tournament == null || !Objects.equals(TournamentStatusEnum.ACTIVE.getCode(), tournament.getTournamentStatus())) {
                continue;
            }
            if (!isCompetitionSeasonValid(tournament, competitionSeasonMap)) {
                continue;
            }

            String registrationStatus = resolveRegistrationStatus(season, tournament, now);
            BasketTournamentRegistration existing = existingMap.get(season.getId());
            AvailableSeasonItemResp resp = buildAvailableSeasonItem(
                    season, tournament, team, existing, registrationStatus, competitionSeasonMap);
            resp.setCanRegister(false);
            fillMemberRemindFields(resp, team, season, tournament, existing, memberId, now);
            items.add(resp);
        }

        AvailableSeasonPageResp page = buildAvailablePage(items, currentPage, pageSize);
        if (items.isEmpty()) {
            page.setEmptyReason("当前暂无可参与赛事");
        }
        return page;
    }

    @Override
    public IPage<TeamEventRecordItemResp> pageMemberTeamEvents(String teamId, Integer currentPage, Integer pageSize) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        requireTeamForMemberView(memberId, teamPk);

        List<BasketTournamentRegistration> registrations = registrationDao.selectList(
                new LambdaQueryWrapper<BasketTournamentRegistration>()
                        .eq(BasketTournamentRegistration::getTeamId, teamPk)
                        .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED)
                        .orderByDesc(BasketTournamentRegistration::getCreateTime));

        Map<Long, BasketTournamentSeason> seasonMap = loadSeasonMap(registrations);
        Map<Long, BasketTournament> tournamentMap = loadTournamentMap(seasonMap.values());
        Map<Long, BasketTournamentRegistrationAudit> latestAuditMap = loadLatestAuditMap(
                registrations.stream().map(BasketTournamentRegistration::getId).collect(Collectors.toList()));

        List<TeamEventRecordItemResp> items = new ArrayList<>();
        for (BasketTournamentRegistration registration : registrations) {
            BasketTournamentSeason season = seasonMap.get(registration.getSeasonId());
            BasketTournament tournament = season == null ? null : tournamentMap.get(season.getTournamentId());
            TournamentRegistrationStatusEnum status = TournamentRegistrationStatusEnum.fromCode(registration.getRegistrationStatus());
            BasketTournamentRegistrationAudit latestAudit = latestAuditMap.get(registration.getId());

            TeamEventRecordItemResp item = new TeamEventRecordItemResp();
            item.setRegistrationId(toStringId(registration.getId()));
            item.setSeasonId(toStringId(registration.getSeasonId()));
            item.setTournamentId(season == null ? null : toStringId(season.getTournamentId()));
            item.setTournamentName(tournament == null ? null : tournament.getTournamentName());
            item.setSeasonName(season == null ? null : season.getSeasonName());
            item.setRegistrationStatus(registration.getRegistrationStatus());
            item.setRegistrationStatusText(status.getDescription());
            item.setAuditStatusText(status.getDescription());
            item.setRosterStatusText(resolveRosterStatusText(registration));
            item.setParticipationStatusText(resolveParticipationStatusText(registration.getRegistrationStatus()));
            item.setSubmitTime(registration.getSubmittedTime());
            item.setAuditTime(latestAudit == null ? null : latestAudit.getAuditTime());
            if (latestAudit != null && Objects.equals(TournamentRegistrationStatusEnum.REJECTED.getCode(), registration.getRegistrationStatus())) {
                item.setRejectReason(latestAudit.getAuditRemark());
            }
            items.add(item);
        }
        return toPage(items, currentPage, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remindCaptainToRegister(String teamId, TeamRegistrationRemindReq req) {
        if (req == null || !StringUtils.hasText(req.getSeasonId())) {
            throw new ServiceException("赛季主键不能为空");
        }
        Long teamPk = parseLongId(teamId, "球队主键");
        Long seasonPk = parseLongId(req.getSeasonId(), "赛季主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = requireTeamForMemberView(memberId, teamPk);
        if (memberId.equals(team.getCaptainMemberId())) {
            throw new ServiceException("队长无需提醒自己", 403);
        }
        if (team.getCaptainMemberId() == null) {
            throw new ServiceException("当前球队暂无可提醒队长");
        }
        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            throw new ServiceException("球队认证通过后才能报名赛事");
        }

        BasketTournamentSeason season = seasonDao.selectById(seasonPk);
        if (season == null || !NOT_DELETED.equals(season.getIsDelete())) {
            throw new ServiceException("赛季不存在");
        }
        BasketTournament tournament = tournamentDao.selectById(season.getTournamentId());
        if (tournament == null || !NOT_DELETED.equals(tournament.getIsDelete())) {
            throw new ServiceException("赛事不存在");
        }

        Date now = new Date();
        String registrationStatus = resolveRegistrationStatus(season, tournament, now);
        if (!"OPEN".equals(registrationStatus)) {
            throw new ServiceException("赛事报名已截止或未开放");
        }

        BasketTournamentRegistration existing = registrationDao.selectOne(new LambdaQueryWrapper<BasketTournamentRegistration>()
                .eq(BasketTournamentRegistration::getTeamId, teamPk)
                .eq(BasketTournamentRegistration::getSeasonId, seasonPk)
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (existing != null && isBlockingRegistration(existing.getRegistrationStatus())) {
            throw new ServiceException("球队已报名该赛事");
        }

        if (hasRecentRemind(memberId, seasonPk)) {
            throw new ServiceException("24小时内已提醒过队长，请稍后再试");
        }

        String terminalMismatchReason = resolveTerminalMismatchReason(team, tournament);
        if (terminalMismatchReason != null) {
            throw new ServiceException(terminalMismatchReason);
        }

        String reminderName = resolveMemberNickname(memberId);
        String tournamentName = tournament.getTournamentName() == null ? season.getSeasonName() : tournament.getTournamentName();
        String content = String.format("队员「%s」提醒你报名赛事「%s」。", reminderName, tournamentName);
        String redirectValue = String.format("/account/my-events/%s/team-registration", seasonPk);
        businessMessageService.sendSiteMessageFrom(
                memberId,
                team.getCaptainMemberId(),
                "赛事报名提醒",
                content,
                BIZ_TYPE_TEAM_REGISTRATION_REMIND,
                seasonPk,
                "ROUTE",
                redirectValue);
    }

    @Override
    public TeamRegistrationDetailResp getMemberDetail(String registrationId) {
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentRegistration registration = registrationDao.selectById(registrationPk);
        if (registration == null || !NOT_DELETED.equals(registration.getIsDelete())) {
            throw new ServiceException("报名记录不存在");
        }
        captainPermissionService.requireTeamMember(memberId, registration.getTeamId());
        BasketTeam team = teamDao.selectById(registration.getTeamId());
        BasketTournamentSeason season = seasonDao.selectById(registration.getSeasonId());
        TeamRegistrationDetailResp resp = buildDetailResp(registration, season, team);
        resp.setCanEdit(false);
        resp.setCanSubmit(false);
        resp.setCanWithdraw(false);
        resp.setCanDeleteDraft(false);
        return resp;
    }

    private BasketTeam requireTeamForMemberView(Long memberId, Long teamPk) {
        captainPermissionService.requireTeamMember(memberId, teamPk);
        BasketTeam team = teamDao.selectById(teamPk);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        if (memberId.equals(team.getCaptainMemberId())) {
            throw new ServiceException("队长请使用球队报名管理接口", 403);
        }
        return team;
    }

    private void fillMemberRemindFields(AvailableSeasonItemResp resp,
                                        BasketTeam team,
                                        BasketTournamentSeason season,
                                        BasketTournament tournament,
                                        BasketTournamentRegistration existing,
                                        Long memberId,
                                        Date now) {
        resp.setCanRemindCaptain(false);
        resp.setRemindedRecently(false);
        if (existing != null && isBlockingRegistration(existing.getRegistrationStatus())) {
            resp.setCannotRemindReason("球队已报名该赛事");
            return;
        }
        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            resp.setCannotRemindReason("球队认证通过后才能报名赛事");
            return;
        }
        String registrationStatus = resolveRegistrationStatus(season, tournament, now);
        if (!"OPEN".equals(registrationStatus)) {
            resp.setCannotRemindReason("赛事报名已截止或未开放");
            return;
        }
        if (team.getCaptainMemberId() == null) {
            resp.setCannotRemindReason("当前球队暂无可提醒队长");
            return;
        }
        String terminalMismatchReason = resolveTerminalMismatchReason(team, tournament);
        if (terminalMismatchReason != null) {
            resp.setCannotRemindReason(terminalMismatchReason);
            return;
        }
        if (hasRecentRemind(memberId, season.getId())) {
            resp.setRemindedRecently(true);
            resp.setCannotRemindReason("24小时内已提醒过队长");
            return;
        }
        resp.setCanRemindCaptain(true);
        resp.setCannotRemindReason(null);
    }

    private boolean hasRecentRemind(Long memberId, Long seasonId) {
        Date threshold = new Date(System.currentTimeMillis() - REMIND_COOLDOWN_MS);
        Long count = messageTaskDao.selectCount(new LambdaQueryWrapper<BasketMessageTask>()
                .eq(BasketMessageTask::getBizType, BIZ_TYPE_TEAM_REGISTRATION_REMIND)
                .eq(BasketMessageTask::getBizId, seasonId)
                .eq(BasketMessageTask::getCreatorId, memberId)
                .eq(BasketMessageTask::getIsDelete, NOT_DELETED)
                .ge(BasketMessageTask::getCreateTime, threshold));
        return count != null && count > 0;
    }

    private String resolveMemberNickname(Long memberId) {
        BasketMemberAccount account = memberAccountDao.selectById(memberId);
        if (account != null && StringUtils.hasText(account.getUsername())) {
            return account.getUsername().trim();
        }
        return "队员";
    }

    private String resolveRosterStatusText(BasketTournamentRegistration registration) {
        Integer count = registration.getLineupCount();
        if (count == null || count <= 0) {
            return "未提交阵容";
        }
        return count + " 人";
    }

    private String resolveParticipationStatusText(Integer registrationStatus) {
        if (Objects.equals(TournamentRegistrationStatusEnum.APPROVED.getCode(), registrationStatus)) {
            return "已正式参赛";
        }
        if (Objects.equals(TournamentRegistrationStatusEnum.SUBMITTED.getCode(), registrationStatus)
                || Objects.equals(TournamentRegistrationStatusEnum.REVIEWING.getCode(), registrationStatus)) {
            return "待审核";
        }
        return TournamentRegistrationStatusEnum.fromCode(registrationStatus).getDescription();
    }

    private Map<Long, BasketTournamentRegistrationAudit> loadLatestAuditMap(List<Long> registrationIds) {
        if (CollectionUtils.isEmpty(registrationIds)) {
            return Collections.emptyMap();
        }
        Map<Long, BasketTournamentRegistrationAudit> map = new HashMap<>();
        for (BasketTournamentRegistrationAudit audit : registrationAuditDao.selectList(
                new LambdaQueryWrapper<BasketTournamentRegistrationAudit>()
                        .in(BasketTournamentRegistrationAudit::getRegistrationId, registrationIds)
                        .eq(BasketTournamentRegistrationAudit::getIsDelete, NOT_DELETED)
                        .orderByDesc(BasketTournamentRegistrationAudit::getAuditTime))) {
            map.putIfAbsent(audit.getRegistrationId(), audit);
        }
        return map;
    }

    @Override
    public IPage<TeamRegistrationItemResp> pageRegistrations(String teamId, Integer currentPage, Integer pageSize, Integer status) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireTeamCaptain(memberId, teamPk);

        LambdaQueryWrapper<BasketTournamentRegistration> wrapper = new LambdaQueryWrapper<BasketTournamentRegistration>()
                .eq(BasketTournamentRegistration::getTeamId, teamPk)
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTournamentRegistration::getCreateTime);
        if (status != null) {
            wrapper.eq(BasketTournamentRegistration::getRegistrationStatus, status);
        }

        Page<BasketTournamentRegistration> page = new Page<>(normalizePage(currentPage), normalizeSize(pageSize));
        IPage<BasketTournamentRegistration> regPage = registrationDao.selectPage(page, wrapper);
        Map<Long, BasketTournamentSeason> seasonMap = loadSeasonMap(regPage.getRecords());
        Map<Long, BasketTournament> tournamentMap = loadTournamentMap(seasonMap.values());

        Page<TeamRegistrationItemResp> respPage = new Page<>(regPage.getCurrent(), regPage.getSize(), regPage.getTotal());
        List<TeamRegistrationItemResp> items = new ArrayList<>();
        for (BasketTournamentRegistration registration : regPage.getRecords()) {
            items.add(toItemResp(registration, seasonMap.get(registration.getSeasonId()),
                    tournamentMap.get(seasonMap.get(registration.getSeasonId()) == null
                            ? null : seasonMap.get(registration.getSeasonId()).getTournamentId())));
        }
        respPage.setRecords(items);
        return respPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamRegistrationDetailResp createDraft(String teamId, TeamRegistrationDraftReq req) {
        if (req == null || !StringUtils.hasText(req.getSeasonId())) {
            throw new ServiceException("赛季主键不能为空");
        }
        Long teamPk = parseLongId(teamId, "球队主键");
        Long seasonPk = parseLongId(req.getSeasonId(), "赛季主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(memberId, teamPk);
        BasketTournamentSeason season = requireOpenSeason(seasonPk);
        BasketTournament tournament = tournamentDao.selectById(season.getTournamentId());
        assertTeamRegistrationEligible(team, tournament);

        assertNoExistingRegistration(teamPk, seasonPk);

        BasketTournamentRegistration registration = new BasketTournamentRegistration();
        registration.setRegistrationNo("RG" + IdUtil.getSnowflakeNextIdStr());
        registration.setSeasonId(seasonPk);
        registration.setTeamId(teamPk);
        registration.setCaptainMemberId(memberId);
        registration.setCaptainPlayerId(team.getCaptainPlayerId());
        registration.setContactName(resolveContactName(trimToNull(req.getContactName()), memberId, team));
        registration.setContactMobile(resolveContactMobile(trimToNull(req.getContactMobile()), memberId));
        registration.setContactEmail(trimToNull(req.getContactEmail()));
        registration.setRemark(trimToNull(req.getRemark()));
        registration.setRegistrationStatus(TournamentRegistrationStatusEnum.DRAFT.getCode());
        registration.setLineupCount(0);
        registration.setIsDelete(NOT_DELETED);
        registrationDao.insert(registration);
        return buildDetailResp(registration, season, team);
    }

    @Override
    public TeamRegistrationDetailResp getDetail(String registrationId) {
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentRegistration registration = captainPermissionService.requireRegistrationCaptain(memberId, registrationPk);
        BasketTeam team = teamDao.selectById(registration.getTeamId());
        BasketTournamentSeason season = seasonDao.selectById(registration.getSeasonId());
        return buildDetailResp(registration, season, team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamRegistrationDetailResp update(String registrationId, TeamRegistrationUpdateReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentRegistration registration = captainPermissionService.requireRegistrationCaptain(memberId, registrationPk);
        assertEditable(registration);

        BasketTeam team = teamDao.selectById(registration.getTeamId());
        registration.setContactName(resolveContactName(trimToNull(req.getContactName()), memberId, team));
        registration.setContactMobile(resolveContactMobile(trimToNull(req.getContactMobile()), memberId));
        registration.setContactEmail(trimToNull(req.getContactEmail()));
        registration.setRemark(trimToNull(req.getRemark()));
        registrationDao.updateById(registration);

        BasketTournamentSeason season = seasonDao.selectById(registration.getSeasonId());
        return buildDetailResp(registration, season, team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamRegistrationDetailResp saveRoster(String registrationId, TeamRegistrationRosterReq req) {
        if (req == null || CollectionUtils.isEmpty(req.getRosterItems())) {
            throw new ServiceException("阵容不能为空");
        }
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentRegistration registration = captainPermissionService.requireRegistrationCaptain(memberId, registrationPk);
        assertEditable(registration);

        BasketTournamentSeason season = seasonDao.selectById(registration.getSeasonId());
        validateRosterSize(req.getRosterItems(), season);

        Set<Long> teamMemberIds = req.getRosterItems().stream()
                .map(item -> parseLongId(item.getTeamMemberId(), "球队成员主键"))
                .collect(Collectors.toSet());
        if (teamMemberIds.size() != req.getRosterItems().size()) {
            throw new ServiceException("阵容成员不能重复");
        }

        Map<Long, BasketTeamMember> memberMap = loadActiveTeamMembers(registration.getTeamId(), teamMemberIds);
        if (memberMap.size() != teamMemberIds.size()) {
            throw new ServiceException("存在无效的球队成员");
        }

        registrationPlayerDao.update(null, new LambdaUpdateWrapper<BasketTournamentRegistrationPlayer>()
                .eq(BasketTournamentRegistrationPlayer::getRegistrationId, registrationPk)
                .eq(BasketTournamentRegistrationPlayer::getIsDelete, NOT_DELETED)
                .set(BasketTournamentRegistrationPlayer::getIsDelete, DELETED));

        int sortNo = 1;
        for (TeamRegistrationRosterItemReq item : req.getRosterItems()) {
            Long teamMemberId = parseLongId(item.getTeamMemberId(), "球队成员主键");
            BasketTeamMember teamMember = memberMap.get(teamMemberId);
            BasketTournamentRegistrationPlayer player = new BasketTournamentRegistrationPlayer();
            player.setRegistrationId(registrationPk);
            player.setSeasonId(registration.getSeasonId());
            player.setTeamId(registration.getTeamId());
            player.setPlayerId(teamMember.getPlayerId());
            player.setMemberId(teamMember.getMemberId());
            player.setTeamMemberId(teamMemberId);
            player.setRosterRole(StringUtils.hasText(item.getRosterRole()) ? item.getRosterRole().trim() : "PLAYER");
            player.setJerseyNo(trimToNull(item.getJerseyNo()));
            player.setIsStarting(item.getIsStarting() == null ? 0 : item.getIsStarting());
            player.setIsCaptain(item.getIsCaptain() == null ? 0 : item.getIsCaptain());
            player.setValidationStatus(RegistrationPlayerValidationStatusEnum.PENDING.getCode());
            player.setSortNo(item.getSortNo() == null ? sortNo++ : item.getSortNo());
            player.setIsDelete(NOT_DELETED);
            registrationPlayerDao.insert(player);
        }

        registration.setLineupCount(req.getRosterItems().size());
        registrationDao.updateById(registration);

        BasketTeam team = teamDao.selectById(registration.getTeamId());
        return buildDetailResp(registration, season, team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamRegistrationDetailResp submit(String registrationId) {
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentRegistration registration = captainPermissionService.requireRegistrationCaptain(memberId, registrationPk);
        TournamentRegistrationStatusEnum status = TournamentRegistrationStatusEnum.fromCode(registration.getRegistrationStatus());
        if (!status.canSubmit()) {
            throw new ServiceException("当前状态不可提交");
        }

        BasketTournamentSeason season = requireOpenSeason(registration.getSeasonId());
        BasketTournament tournament = tournamentDao.selectById(season.getTournamentId());
        BasketTeam team = teamDao.selectById(registration.getTeamId());
        assertTeamRegistrationEligible(team, tournament);
        List<BasketTournamentRegistrationPlayer> roster = loadActiveRoster(registrationPk);
        validateSubmitRoster(registration, season, roster);
        assertNoActiveDiscipline(roster, registration.getSeasonId());

        Date now = new Date();
        int updated = registrationDao.update(null, new LambdaUpdateWrapper<BasketTournamentRegistration>()
                .eq(BasketTournamentRegistration::getId, registrationPk)
                .eq(BasketTournamentRegistration::getCaptainMemberId, memberId)
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED)
                .in(BasketTournamentRegistration::getRegistrationStatus,
                        TournamentRegistrationStatusEnum.DRAFT.getCode(),
                        TournamentRegistrationStatusEnum.REJECTED.getCode())
                .set(BasketTournamentRegistration::getRegistrationStatus, TournamentRegistrationStatusEnum.REVIEWING.getCode())
                .set(BasketTournamentRegistration::getSubmittedTime, now));
        if (updated == 0) {
            throw new ServiceException("提交失败，报名状态已变更");
        }
        markRosterValidationPassed(registrationPk);
        registration = registrationDao.selectById(registrationPk);

        businessMessageService.sendSiteMessage(memberId, "赛事报名审核中",
                "报名已提交，正在等待后台审核", BIZ_TYPE_TEAM_REGISTRATION, registration.getId(),
                "TEAM_REGISTRATION", String.valueOf(registration.getId()));

        return buildDetailResp(registration, season, team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamRegistrationDetailResp withdraw(String registrationId, TeamRegistrationWithdrawReq req) {
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentRegistration registration = captainPermissionService.requireRegistrationCaptain(memberId, registrationPk);
        TournamentRegistrationStatusEnum status = TournamentRegistrationStatusEnum.fromCode(registration.getRegistrationStatus());
        if (!status.canWithdraw()) {
            throw new ServiceException("当前状态不可撤回");
        }

        BasketTournamentSeason season = seasonDao.selectById(registration.getSeasonId());
        Date now = new Date();
        if (season != null && season.getRegistrationEndTime() != null && season.getRegistrationEndTime().before(now)) {
            throw new ServiceException("已过报名截止时间，无法撤回");
        }

        int updated = registrationDao.update(null, new LambdaUpdateWrapper<BasketTournamentRegistration>()
                .eq(BasketTournamentRegistration::getId, registrationPk)
                .eq(BasketTournamentRegistration::getCaptainMemberId, memberId)
                .in(BasketTournamentRegistration::getRegistrationStatus,
                        TournamentRegistrationStatusEnum.REVIEWING.getCode(),
                        TournamentRegistrationStatusEnum.SUBMITTED.getCode())
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED)
                .set(BasketTournamentRegistration::getRegistrationStatus, TournamentRegistrationStatusEnum.WITHDRAWN.getCode())
                .set(BasketTournamentRegistration::getWithdrawalTime, now)
                .set(BasketTournamentRegistration::getWithdrawalReason, req == null ? null : trimToNull(req.getWithdrawalReason())));
        if (updated == 0) {
            throw new ServiceException("撤回失败，报名状态已变更");
        }
        registration = registrationDao.selectById(registrationPk);

        BasketTeam team = teamDao.selectById(registration.getTeamId());
        return buildDetailResp(registration, season, team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(String registrationId) {
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentRegistration registration = captainPermissionService.requireRegistrationCaptain(memberId, registrationPk);
        if (!TournamentRegistrationStatusEnum.fromCode(registration.getRegistrationStatus()).canDeleteDraft()) {
            throw new ServiceException("仅草稿可删除");
        }
        registration.setIsDelete(DELETED);
        registrationDao.updateById(registration);
        registrationPlayerDao.update(null, new LambdaUpdateWrapper<BasketTournamentRegistrationPlayer>()
                .eq(BasketTournamentRegistrationPlayer::getRegistrationId, registrationPk)
                .set(BasketTournamentRegistrationPlayer::getIsDelete, DELETED));
    }

    @Override
    public IPage<TeamRegistrationAuditItemResp> pageAuditRecords(String registrationId, Integer currentPage, Integer pageSize) {
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireRegistrationCaptain(memberId, registrationPk);

        Page<BasketTournamentRegistrationAudit> page = new Page<>(normalizePage(currentPage), normalizeSize(pageSize));
        IPage<BasketTournamentRegistrationAudit> auditPage = registrationAuditDao.selectPage(page,
                new LambdaQueryWrapper<BasketTournamentRegistrationAudit>()
                        .eq(BasketTournamentRegistrationAudit::getRegistrationId, registrationPk)
                        .eq(BasketTournamentRegistrationAudit::getIsDelete, NOT_DELETED)
                        .orderByDesc(BasketTournamentRegistrationAudit::getAuditTime));

        Page<TeamRegistrationAuditItemResp> respPage = new Page<>(auditPage.getCurrent(), auditPage.getSize(), auditPage.getTotal());
        List<TeamRegistrationAuditItemResp> items = new ArrayList<>();
        for (BasketTournamentRegistrationAudit audit : auditPage.getRecords()) {
            TeamRegistrationAuditItemResp resp = new TeamRegistrationAuditItemResp();
            resp.setAuditId(toStringId(audit.getId()));
            resp.setAuditAction(audit.getAuditAction());
            resp.setBeforeStatus(audit.getBeforeStatus());
            resp.setAfterStatus(audit.getAfterStatus());
            resp.setAuditorName(audit.getAuditorName());
            resp.setAuditRemark(audit.getAuditRemark());
            resp.setAuditTime(audit.getAuditTime());
            items.add(resp);
        }
        respPage.setRecords(items);
        return respPage;
    }

    private void assertNoExistingRegistration(Long teamId, Long seasonId) {
        List<Integer> blockingStatuses = Arrays.asList(
                TournamentRegistrationStatusEnum.DRAFT.getCode(),
                TournamentRegistrationStatusEnum.SUBMITTED.getCode(),
                TournamentRegistrationStatusEnum.REVIEWING.getCode(),
                TournamentRegistrationStatusEnum.APPROVED.getCode(),
                TournamentRegistrationStatusEnum.REJECTED.getCode());
        Long count = registrationDao.selectCount(new LambdaQueryWrapper<BasketTournamentRegistration>()
                .eq(BasketTournamentRegistration::getTeamId, teamId)
                .eq(BasketTournamentRegistration::getSeasonId, seasonId)
                .in(BasketTournamentRegistration::getRegistrationStatus, blockingStatuses)
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("该赛季已存在报名记录");
        }
    }

    private BasketTournamentSeason requireOpenSeason(Long seasonId) {
        BasketTournamentSeason season = seasonDao.selectById(seasonId);
        if (season == null || !NOT_DELETED.equals(season.getIsDelete())) {
            throw new ServiceException("赛季不存在");
        }
        BasketTournament tournament = tournamentDao.selectById(season.getTournamentId());
        if (tournament == null || !NOT_DELETED.equals(tournament.getIsDelete())) {
            throw new ServiceException("赛事不存在");
        }
        if (!Objects.equals(TournamentStatusEnum.ACTIVE.getCode(), tournament.getTournamentStatus())) {
            throw new ServiceException("赛事未发布或未开放报名");
        }
        if (Objects.equals(TournamentSeasonStatusEnum.FINISHED.getCode(), season.getSeasonStatus())
                || Objects.equals(TournamentSeasonStatusEnum.CLOSED.getCode(), season.getSeasonStatus())
                || Objects.equals(TournamentSeasonStatusEnum.CANCELLED.getCode(), season.getSeasonStatus())) {
            throw new ServiceException("赛季未开放报名");
        }
        Date now = new Date();
        if (season.getRegistrationStartTime() == null || season.getRegistrationEndTime() == null) {
            throw new ServiceException("赛事报名时间未配置");
        }
        if (season.getRegistrationStartTime().after(now)) {
            throw new ServiceException("报名尚未开始");
        }
        if (season.getRegistrationEndTime().before(now)) {
            throw new ServiceException("报名已截止");
        }
        if (Integer.valueOf(0).equals(season.getAllowTeamRegistration())) {
            throw new ServiceException("该赛季未开放球队报名");
        }
        if (Objects.equals(TournamentSeasonStatusEnum.PREPARE.getCode(), season.getSeasonStatus())) {
            seasonDao.update(null, new LambdaUpdateWrapper<BasketTournamentSeason>()
                    .eq(BasketTournamentSeason::getId, season.getId())
                    .eq(BasketTournamentSeason::getSeasonStatus, TournamentSeasonStatusEnum.PREPARE.getCode())
                    .set(BasketTournamentSeason::getSeasonStatus, TournamentSeasonStatusEnum.REGISTRATION_OPEN.getCode()));
            season.setSeasonStatus(TournamentSeasonStatusEnum.REGISTRATION_OPEN.getCode());
        }
        return season;
    }

    private void assertTeamRegistrationEligible(BasketTeam team, BasketTournament tournament) {
        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            throw new ServiceException("当前球队尚未认证，认证通过后可报名赛事");
        }
        String terminalMismatchReason = resolveTerminalMismatchReason(team, tournament);
        if (terminalMismatchReason != null) {
            throw new ServiceException(terminalMismatchReason);
        }
    }

    private AvailableSeasonItemResp buildAvailableSeasonItem(BasketTournamentSeason season,
                                                             BasketTournament tournament,
                                                             BasketTeam team,
                                                             BasketTournamentRegistration existing,
                                                             String registrationStatus,
                                                             Map<Long, BasketCompetitionSeason> competitionSeasonMap) {
        AvailableSeasonItemResp resp = new AvailableSeasonItemResp();
        resp.setSeasonId(toStringId(season.getId()));
        resp.setSeasonNo(season.getSeasonNo());
        resp.setSeasonName(season.getSeasonName());
        resp.setCompetitionSeasonId(tournament.getCompetitionSeasonId());
        if (tournament.getCompetitionSeasonId() != null) {
            BasketCompetitionSeason competitionSeason = competitionSeasonMap.get(tournament.getCompetitionSeasonId());
            if (competitionSeason != null) {
                resp.setCompetitionSeasonName(competitionSeason.getSeasonName());
            }
        }
        resp.setTournamentId(toStringId(season.getTournamentId()));
        resp.setTournamentNo(tournament.getTournamentNo());
        resp.setTournamentName(tournament.getTournamentName());
        resp.setGameTerminal(tournament.getGameTerminal());
        resp.setTournamentStatus(tournament.getTournamentStatus());
        resp.setSeasonStatus(season.getSeasonStatus());
        resp.setRegistrationStatus(registrationStatus);
        resp.setTeamId(toStringId(team.getId()));
        resp.setTeamName(team.getTeamName());
        resp.setTeamCertificationStatus(team.getAuditStatus());
        resp.setMinRosterCount(season.getMinRosterCount());
        resp.setMaxRosterCount(season.getMaxRosterCount());
        resp.setRegistrationStartTime(season.getRegistrationStartTime());
        resp.setRegistrationEndTime(season.getRegistrationEndTime());
        resp.setAlreadyRegistered(existing != null);
        resp.setHasRegistration(existing != null);
        resp.setRegistrationId(existing == null ? null : toStringId(existing.getId()));
        resp.setAuditStatus(existing == null ? null : existing.getRegistrationStatus());
        return resp;
    }

    private AvailableSeasonPageResp buildAvailablePage(List<AvailableSeasonItemResp> items,
                                                       Integer currentPage,
                                                       Integer pageSize) {
        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);
        long total = items.size();
        int from = (int) Math.min((page - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        List<AvailableSeasonItemResp> records = from >= to ? Collections.emptyList() : items.subList(from, to);
        AvailableSeasonPageResp respPage = new AvailableSeasonPageResp(page, size, total);
        respPage.setRecords(records);
        return respPage;
    }

    private String resolveRegistrationStatus(BasketTournamentSeason season, BasketTournament tournament, Date now) {
        if (Objects.equals(TournamentStatusEnum.ENDED.getCode(), tournament.getTournamentStatus())) {
            return "ENDED";
        }
        if (season.getRegistrationStartTime() == null || season.getRegistrationEndTime() == null) {
            return "CLOSED";
        }
        if (now.before(season.getRegistrationStartTime())) {
            return "NOT_STARTED";
        }
        if (now.after(season.getRegistrationEndTime())) {
            return "CLOSED";
        }
        return "OPEN";
    }

    private String resolveEmptyReason(BasketTeam team,
                                      int inWindowCount,
                                      int notStartedCount,
                                      int terminalMismatchCount,
                                      int alreadyRegisteredCount,
                                      int candidateCount) {
        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            return "当前球队尚未认证，认证通过后可报名赛事";
        }
        if (notStartedCount > 0 && inWindowCount == 0) {
            return "当前赛事报名尚未开始，请在报名开始时间后再试";
        }
        if (candidateCount == 0 || inWindowCount == 0) {
            return "当前没有处于报名期的赛事";
        }
        if (terminalMismatchCount > 0 && terminalMismatchCount >= inWindowCount) {
            return "当前球队比赛终端暂无可报名赛事";
        }
        if (alreadyRegisteredCount > 0 && alreadyRegisteredCount >= inWindowCount) {
            return "当前球队已报名全部开放赛事";
        }
        return "当前暂无可报名赛事";
    }

    private boolean isCompetitionSeasonValid(BasketTournament tournament,
                                             Map<Long, BasketCompetitionSeason> competitionSeasonMap) {
        if (tournament.getCompetitionSeasonId() == null) {
            return true;
        }
        BasketCompetitionSeason competitionSeason = competitionSeasonMap.get(tournament.getCompetitionSeasonId());
        return competitionSeason != null && NOT_DELETED.equals(competitionSeason.getIsDelete());
    }

    private String resolveTerminalMismatchReason(BasketTeam team, BasketTournament tournament) {
        if (!StringUtils.hasText(tournament.getGameTerminal())) {
            return null;
        }
        if (!StringUtils.hasText(team.getGameTerminal())) {
            return "当前球队比赛终端未设置，无法报名该赛事";
        }
        if (!team.getGameTerminal().trim().equalsIgnoreCase(tournament.getGameTerminal().trim())) {
            return "当前球队比赛终端与赛事终端不一致";
        }
        return null;
    }

    private String resolveQuotaExceededReason(BasketTournamentSeason season, Map<Long, Long> seasonRegistrationCountMap) {
        if (season.getMaxTeamCount() == null || season.getMaxTeamCount() <= 0) {
            return null;
        }
        Long count = seasonRegistrationCountMap.getOrDefault(season.getId(), 0L);
        if (count >= season.getMaxTeamCount()) {
            return "该赛事报名名额已满";
        }
        return null;
    }

    private boolean isBlockingRegistration(Integer registrationStatus) {
        if (registrationStatus == null) {
            return false;
        }
        return Arrays.asList(
                TournamentRegistrationStatusEnum.DRAFT.getCode(),
                TournamentRegistrationStatusEnum.SUBMITTED.getCode(),
                TournamentRegistrationStatusEnum.REVIEWING.getCode(),
                TournamentRegistrationStatusEnum.APPROVED.getCode(),
                TournamentRegistrationStatusEnum.REJECTED.getCode()).contains(registrationStatus);
    }

    private Map<Long, BasketCompetitionSeason> loadCompetitionSeasonMap(java.util.Collection<BasketTournament> tournaments) {
        Set<Long> competitionSeasonIds = tournaments.stream()
                .map(BasketTournament::getCompetitionSeasonId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (competitionSeasonIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BasketCompetitionSeason> map = new HashMap<>();
        for (BasketCompetitionSeason competitionSeason : competitionSeasonDao.selectBatchIds(competitionSeasonIds)) {
            if (competitionSeason != null && NOT_DELETED.equals(competitionSeason.getIsDelete())) {
                map.put(competitionSeason.getId(), competitionSeason);
            }
        }
        return map;
    }

    private Map<Long, Long> loadSeasonRegistrationCountMap(List<BasketTournamentSeason> seasons) {
        if (CollectionUtils.isEmpty(seasons)) {
            return Collections.emptyMap();
        }
        Set<Long> seasonIds = seasons.stream().map(BasketTournamentSeason::getId).collect(Collectors.toSet());
        Map<Long, Long> map = new HashMap<>();
        for (BasketTournamentRegistration registration : registrationDao.selectList(new LambdaQueryWrapper<BasketTournamentRegistration>()
                .in(BasketTournamentRegistration::getSeasonId, seasonIds)
                .in(BasketTournamentRegistration::getRegistrationStatus, Arrays.asList(
                        TournamentRegistrationStatusEnum.SUBMITTED.getCode(),
                        TournamentRegistrationStatusEnum.REVIEWING.getCode(),
                        TournamentRegistrationStatusEnum.APPROVED.getCode()))
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED))) {
            map.merge(registration.getSeasonId(), 1L, Long::sum);
        }
        return map;
    }

    private String resolveContactMobile(String contactMobile, Long memberId) {
        if (StringUtils.hasText(contactMobile)) {
            return contactMobile.trim();
        }
        BasketMemberAccount account = memberAccountDao.selectById(memberId);
        if (account != null && StringUtils.hasText(account.getEmail())) {
            return account.getEmail().trim();
        }
        return "";
    }

    private String resolveContactName(String contactName, Long memberId, BasketTeam team) {
        if (StringUtils.hasText(contactName)) {
            return contactName.trim();
        }
        if (team != null && StringUtils.hasText(team.getTeamName())) {
            return team.getTeamName().trim();
        }
        BasketMemberAccount account = memberAccountDao.selectById(memberId);
        if (account != null && StringUtils.hasText(account.getUsername())) {
            return account.getUsername().trim();
        }
        return "联系人";
    }

    private void assertEditable(BasketTournamentRegistration registration) {
        if (!TournamentRegistrationStatusEnum.fromCode(registration.getRegistrationStatus()).canEdit()) {
            throw new ServiceException("当前状态不可编辑");
        }
    }

    private void validateRosterSize(List<TeamRegistrationRosterItemReq> items, BasketTournamentSeason season) {
        int size = items.size();
        if (season.getMinRosterCount() != null && size < season.getMinRosterCount()) {
            throw new ServiceException("阵容人数不足");
        }
        if (season.getMaxRosterCount() != null && size > season.getMaxRosterCount()) {
            throw new ServiceException("阵容人数超出上限");
        }
    }

    private Map<Long, BasketTeamMember> loadActiveTeamMembers(Long teamId, Set<Long> teamMemberIds) {
        Map<Long, BasketTeamMember> map = new HashMap<>();
        for (BasketTeamMember member : teamMemberDao.selectBatchIds(teamMemberIds)) {
            if (member != null && NOT_DELETED.equals(member.getIsDelete())
                    && teamId.equals(member.getTeamId())
                    && Integer.valueOf(IS_CURRENT).equals(member.getIsCurrent())
                    && Objects.equals(TeamMembershipStatusEnum.ACTIVE.getCode(), member.getMembershipStatus())) {
                map.put(member.getId(), member);
            }
        }
        return map;
    }

    private List<BasketTournamentRegistrationPlayer> loadActiveRoster(Long registrationId) {
        return registrationPlayerDao.selectList(new LambdaQueryWrapper<BasketTournamentRegistrationPlayer>()
                .eq(BasketTournamentRegistrationPlayer::getRegistrationId, registrationId)
                .eq(BasketTournamentRegistrationPlayer::getIsDelete, NOT_DELETED)
                .orderByAsc(BasketTournamentRegistrationPlayer::getSortNo));
    }

    private void validateSubmitRoster(BasketTournamentRegistration registration,
                                      BasketTournamentSeason season,
                                      List<BasketTournamentRegistrationPlayer> roster) {
        validateRosterSize(roster.stream().map(r -> {
            TeamRegistrationRosterItemReq req = new TeamRegistrationRosterItemReq();
            req.setTeamMemberId(toStringId(r.getTeamMemberId()));
            return req;
        }).collect(Collectors.toList()), season);

        long captainCount = roster.stream().filter(r -> Integer.valueOf(IS_CAPTAIN).equals(r.getIsCaptain())).count();
        if (captainCount != 1) {
            throw new ServiceException("阵容中必须且只能有一名场上队长");
        }

        BasketTeamMember captainMember = teamMemberDao.selectOne(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, registration.getTeamId())
                .eq(BasketTeamMember::getMemberId, registration.getCaptainMemberId())
                .eq(BasketTeamMember::getRoleType, TeamMemberRoleEnum.CAPTAIN.getCode())
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (captainMember == null) {
            throw new ServiceException("队长不在球队中");
        }
        boolean captainInRoster = roster.stream().anyMatch(r ->
                captainMember.getId().equals(r.getTeamMemberId()) && Integer.valueOf(IS_CAPTAIN).equals(r.getIsCaptain()));
        if (!captainInRoster) {
            throw new ServiceException("队长必须出现在阵容中并标记为场上队长");
        }

        long startingCount = roster.stream().filter(r -> Integer.valueOf(IS_STARTING).equals(r.getIsStarting())).count();
        if (startingCount != REQUIRED_STARTING_COUNT) {
            throw new ServiceException("5V5 赛制首发人数必须为 5 人");
        }
    }

    private void markRosterValidationPassed(Long registrationPk) {
        registrationPlayerDao.update(null, new LambdaUpdateWrapper<BasketTournamentRegistrationPlayer>()
                .eq(BasketTournamentRegistrationPlayer::getRegistrationId, registrationPk)
                .eq(BasketTournamentRegistrationPlayer::getIsDelete, NOT_DELETED)
                .set(BasketTournamentRegistrationPlayer::getValidationStatus,
                        RegistrationPlayerValidationStatusEnum.PASS.getCode())
                .set(BasketTournamentRegistrationPlayer::getRejectReason, null));
    }

    private void assertNoActiveDiscipline(List<BasketTournamentRegistrationPlayer> roster, Long seasonId) {
        Date now = new Date();
        Set<Long> memberIds = roster.stream().map(BasketTournamentRegistrationPlayer::getMemberId).collect(Collectors.toSet());
        Set<Long> playerIds = roster.stream().map(BasketTournamentRegistrationPlayer::getPlayerId).collect(Collectors.toSet());
        if (memberIds.isEmpty()) {
            return;
        }
        List<BasketDisciplineRecord> records = disciplineRecordDao.selectList(new LambdaQueryWrapper<BasketDisciplineRecord>()
                .eq(BasketDisciplineRecord::getSeasonId, seasonId)
                .eq(BasketDisciplineRecord::getDisciplineStatus, DisciplineStatusEnum.ACTIVE.getCode())
                .eq(BasketDisciplineRecord::getIsDelete, NOT_DELETED)
                .and(w -> w.in(BasketDisciplineRecord::getMemberId, memberIds)
                        .or().in(BasketDisciplineRecord::getPlayerId, playerIds)));
        for (BasketDisciplineRecord record : records) {
            if (isDisciplineEffective(record, now)) {
                throw new ServiceException("阵容中存在处于处罚期的球员，无法提交报名");
            }
        }
    }

    private boolean isDisciplineEffective(BasketDisciplineRecord record, Date now) {
        if (record.getEffectiveStartTime() != null && record.getEffectiveStartTime().after(now)) {
            return false;
        }
        if (record.getEffectiveEndTime() != null && record.getEffectiveEndTime().before(now)) {
            return false;
        }
        return true;
    }

    private TeamRegistrationDetailResp buildDetailResp(BasketTournamentRegistration registration,
                                                       BasketTournamentSeason season,
                                                       BasketTeam team) {
        TournamentRegistrationStatusEnum status = TournamentRegistrationStatusEnum.fromCode(registration.getRegistrationStatus());
        TeamRegistrationDetailResp resp = new TeamRegistrationDetailResp();
        resp.setRegistrationId(toStringId(registration.getId()));
        resp.setRegistrationNo(registration.getRegistrationNo());
        resp.setSeasonId(toStringId(registration.getSeasonId()));
        resp.setSeasonName(season == null ? null : season.getSeasonName());
        resp.setTeamId(toStringId(registration.getTeamId()));
        resp.setTeamName(team == null ? null : team.getTeamName());
        resp.setContactName(registration.getContactName());
        resp.setContactMobileMasked(DataMaskUtil.maskMobile(registration.getContactMobile()));
        resp.setContactEmailMasked(DataMaskUtil.maskEmail(registration.getContactEmail()));
        resp.setRegistrationStatus(registration.getRegistrationStatus());
        resp.setRegistrationStatusText(status.getDisplayDescription());
        resp.setLineupCount(registration.getLineupCount());
        resp.setRemark(registration.getRemark());
        resp.setCanEdit(status.canEdit());
        resp.setCanSubmit(status.canSubmit());
        resp.setCanWithdraw(status.canWithdraw());
        resp.setCanDeleteDraft(status.canDeleteDraft());
        resp.setSubmittedTime(registration.getSubmittedTime());
        resp.setRegistrationEndTime(season == null ? null : season.getRegistrationEndTime());

        if (season != null) {
            BasketTournament tournament = tournamentDao.selectById(season.getTournamentId());
            resp.setTournamentName(tournament == null ? null : tournament.getTournamentName());
        }

        List<BasketTournamentRegistrationPlayer> roster = loadActiveRoster(registration.getId());
        Map<Long, BasketPlayerProfile> playerMap = loadPlayerMap(roster);
        List<TeamRegistrationPlayerItemResp> rosterItems = new ArrayList<>();
        for (BasketTournamentRegistrationPlayer player : roster) {
            TeamRegistrationPlayerItemResp item = new TeamRegistrationPlayerItemResp();
            item.setRegistrationPlayerId(toStringId(player.getId()));
            item.setTeamMemberId(toStringId(player.getTeamMemberId()));
            item.setMemberId(toStringId(player.getMemberId()));
            item.setUserId(toStringId(player.getMemberId()));
            item.setPlayerId(toStringId(player.getPlayerId()));
            item.setJerseyNo(player.getJerseyNo());
            item.setIsStarting(player.getIsStarting());
            item.setIsCaptain(player.getIsCaptain());
            item.setValidationStatus(player.getValidationStatus());
            item.setSortNo(player.getSortNo());
            BasketPlayerProfile profile = playerMap.get(player.getPlayerId());
            if (profile != null) {
                item.setGameNickname(profile.getGameNickname());
                item.setPositionCode(profile.getPositionCode());
            }
            rosterItems.add(item);
        }
        resp.setRosterItems(rosterItems);
        return resp;
    }

    private TeamRegistrationItemResp toItemResp(BasketTournamentRegistration registration,
                                                BasketTournamentSeason season,
                                                BasketTournament tournament) {
        TournamentRegistrationStatusEnum status = TournamentRegistrationStatusEnum.fromCode(registration.getRegistrationStatus());
        TeamRegistrationItemResp resp = new TeamRegistrationItemResp();
        resp.setRegistrationId(toStringId(registration.getId()));
        resp.setRegistrationNo(registration.getRegistrationNo());
        resp.setSeasonId(toStringId(registration.getSeasonId()));
        resp.setSeasonName(season == null ? null : season.getSeasonName());
        resp.setTournamentName(tournament == null ? null : tournament.getTournamentName());
        resp.setRegistrationStatus(registration.getRegistrationStatus());
        resp.setRegistrationStatusText(status.getDisplayDescription());
        resp.setLineupCount(registration.getLineupCount());
        resp.setCanEdit(status.canEdit());
        resp.setCanSubmit(status.canSubmit());
        resp.setCanWithdraw(status.canWithdraw());
        resp.setSubmittedTime(registration.getSubmittedTime());
        resp.setCreateTime(registration.getCreateTime());
        return resp;
    }

    private Map<Long, BasketTournamentRegistration> loadExistingRegistrationMap(Long teamId, List<BasketTournamentSeason> seasons) {
        if (CollectionUtils.isEmpty(seasons)) {
            return Collections.emptyMap();
        }
        Set<Long> seasonIds = seasons.stream().map(BasketTournamentSeason::getId).collect(Collectors.toSet());
        Map<Long, BasketTournamentRegistration> map = new HashMap<>();
        for (BasketTournamentRegistration registration : registrationDao.selectList(new LambdaQueryWrapper<BasketTournamentRegistration>()
                .eq(BasketTournamentRegistration::getTeamId, teamId)
                .in(BasketTournamentRegistration::getSeasonId, seasonIds)
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED))) {
            map.putIfAbsent(registration.getSeasonId(), registration);
        }
        return map;
    }

    private Map<Long, BasketTournamentSeason> loadSeasonMap(List<BasketTournamentRegistration> registrations) {
        Set<Long> seasonIds = registrations.stream().map(BasketTournamentRegistration::getSeasonId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (seasonIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BasketTournamentSeason> map = new HashMap<>();
        for (BasketTournamentSeason season : seasonDao.selectBatchIds(seasonIds)) {
            if (season != null && NOT_DELETED.equals(season.getIsDelete())) {
                map.put(season.getId(), season);
            }
        }
        return map;
    }

    private Map<Long, BasketTournament> loadTournamentMap(java.util.Collection<BasketTournamentSeason> seasons) {
        Set<Long> tournamentIds = seasons.stream().map(BasketTournamentSeason::getTournamentId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (tournamentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BasketTournament> map = new HashMap<>();
        for (BasketTournament tournament : tournamentDao.selectBatchIds(tournamentIds)) {
            if (tournament != null && NOT_DELETED.equals(tournament.getIsDelete())) {
                map.put(tournament.getId(), tournament);
            }
        }
        return map;
    }

    private Map<Long, BasketPlayerProfile> loadPlayerMap(List<BasketTournamentRegistrationPlayer> roster) {
        Set<Long> playerIds = roster.stream().map(BasketTournamentRegistrationPlayer::getPlayerId).collect(Collectors.toSet());
        if (playerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BasketPlayerProfile> map = new HashMap<>();
        for (BasketPlayerProfile profile : playerProfileDao.selectBatchIds(playerIds)) {
            if (profile != null && NOT_DELETED.equals(profile.getIsDelete())) {
                map.put(profile.getId(), profile);
            }
        }
        return map;
    }

    private <T> IPage<T> toPage(List<T> items, Integer currentPage, Integer pageSize) {
        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);
        long total = items.size();
        int from = (int) Math.min((page - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        List<T> records = from >= to ? Collections.emptyList() : items.subList(from, to);
        Page<T> respPage = new Page<>(page, size, total);
        respPage.setRecords(records);
        return respPage;
    }

    private long normalizePage(Integer currentPage) {
        return currentPage == null || currentPage < 1 ? 1L : currentPage;
    }

    private long normalizeSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100L);
    }

    private Long parseLongId(String id, String fieldName) {
        if (!StringUtils.hasText(id)) {
            throw new ServiceException(fieldName + "不能为空");
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(fieldName + "格式不正确");
        }
    }

    private String toStringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
