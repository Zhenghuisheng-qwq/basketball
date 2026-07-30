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
import com.zhs.model.*;
import com.zhs.request.account.MyEventsSeasonPageQuery;
import com.zhs.request.team.*;
import com.zhs.response.account.CompetitionSeasonOptionResp;
import com.zhs.response.account.MyEventsContextResp;
import com.zhs.response.team.*;
import com.zhs.service.IBusinessMessageService;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.ITeamRegistrationBusinessService;
import com.zhs.support.*;
import com.zhs.util.DataMaskUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamRegistrationBusinessServiceImpl implements ITeamRegistrationBusinessService {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;
    private static final int IS_CAPTAIN = 1;
    private static final int IS_STARTING = 1;
    private static final int REQUIRED_STARTING_COUNT = 5;
    private static final int DEFAULT_MIN_ROSTER_COUNT = 5;
    private static final int DEFAULT_MAX_ROSTER_COUNT = 8;
    private static final int DELETED = 1;
    private static final int ENABLED = 1;
    private static final String BIZ_TYPE_TEAM_REGISTRATION = "TEAM_REGISTRATION";
    private static final String BIZ_TYPE_TEAM_REGISTRATION_REMIND = "TEAM_REGISTRATION_REMIND";
    private static final String ROSTER_LOCKED_MESSAGE = "当前报名已审核通过，阵容已锁定，不能直接修改。如需调整，请重新提交审核或联系赛事管理员。";
    private static final String ROSTER_LOCKED_HINT = "报名已审核通过，阵容已锁定。如需调整，请重新提交审核或联系赛事管理员。";
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
    @Resource
    private EventPlatformSupport eventPlatformSupport;
    @Resource
    private CaptainGrantSupport captainGrantSupport;
    @Resource
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private PlatformTeamMembershipSupport platformTeamMembershipSupport;
    @Resource
    private BasketTeamSeasonProfileDao teamSeasonProfileDao;

    @Override
    public List<SeasonPlatformItemResp> listSeasonPlatforms(String seasonId) {
        Long seasonPk = parseLongId(seasonId, "赛季主键");
        requireOpenSeason(seasonPk);
        return eventPlatformSupport.listSeasonPlatforms(seasonPk);
    }

    @Override
    public List<RegistrationRosterCandidateItemResp> listRosterCandidates(String registrationId) {
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentRegistration registration = captainPermissionService.requireRegistrationCaptain(memberId, registrationPk);
        String platformCode = registration.getPlatformCode();
        if (!StringUtils.hasText(platformCode)) {
            throw new ServiceException("报名记录缺少平台信息");
        }

        List<BasketTeamMember> members = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, registration.getTeamId())
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByAsc(BasketTeamMember::getRoleType)
                .orderByAsc(BasketTeamMember::getJoinTime));
        if (CollectionUtils.isEmpty(members)) {
            return Collections.emptyList();
        }

        Set<Long> playerIds = members.stream().map(BasketTeamMember::getPlayerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> memberIds = members.stream().map(BasketTeamMember::getMemberId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, BasketPlayerProfile> playerMap = new HashMap<>();
        for (BasketPlayerProfile profile : playerProfileDao.selectBatchIds(playerIds)) {
            if (profile != null && NOT_DELETED.equals(profile.getIsDelete())) {
                playerMap.put(profile.getId(), profile);
            }
        }
        Map<Long, BasketMemberProfile> memberProfileMap = new HashMap<>();
        for (BasketMemberProfile profile : memberProfileDao.selectList(new LambdaQueryWrapper<BasketMemberProfile>()
                .in(BasketMemberProfile::getMemberId, memberIds)
                .eq(BasketMemberProfile::getIsDelete, NOT_DELETED))) {
            memberProfileMap.put(profile.getMemberId(), profile);
        }
        Map<Long, BasketPlayerPlatformAccount> platformAccountMap = new HashMap<>();
        for (BasketPlayerPlatformAccount account : platformAccountDao.selectList(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .in(BasketPlayerPlatformAccount::getPlayerId, playerIds)
                .eq(BasketPlayerPlatformAccount::getPlatform, platformCode)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED))) {
            platformAccountMap.putIfAbsent(account.getPlayerId(), account);
        }

        List<RegistrationRosterCandidateItemResp> items = new ArrayList<>();
        for (BasketTeamMember member : members) {
            BasketPlayerProfile profile = playerMap.get(member.getPlayerId());
            RegistrationRosterCandidateItemResp item = new RegistrationRosterCandidateItemResp();
            item.setTeamMemberId(toStringId(member.getId()));
            item.setPlayerId(toStringId(member.getPlayerId()));
            item.setJerseyNo(member.getJerseyNo());
            item.setRoleType(member.getRoleType());
            if (profile != null) {
                item.setPlayerNo(profile.getPlayerNo());
                item.setGameNickname(profile.getGameNickname());
                item.setPositionCode(profile.getPositionCode());
                item.setPositionName(com.zhs.support.PlayerPositionSupport.resolveName(profile.getPositionCode()));
            }
            BasketMemberProfile memberProfile = memberProfileMap.get(member.getMemberId());
            if (memberProfile != null) {
                item.setNickname(memberProfile.getNickname());
            }
            BasketPlayerPlatformAccount platformAccount = platformAccountMap.get(member.getPlayerId());
            PlatformIdentityStatusEnum identityStatus = platformTeamMembershipSupport.resolvePlatformIdentityStatus(
                    member.getMemberId(), platformCode);
            if (platformAccount != null) {
                item.setPlatformGameId(platformAccount.getDisplayGameId());
                if (Objects.equals(PlatformAccountStatusEnum.ACTIVE.getCode(), platformAccount.getAccountStatus())) {
                    identityStatus = PlatformIdentityStatusEnum.ACTIVE;
                } else if (Objects.equals(PlatformAccountStatusEnum.REJECTED.getCode(), platformAccount.getAccountStatus())) {
                    identityStatus = PlatformIdentityStatusEnum.REJECTED;
                } else {
                    identityStatus = PlatformIdentityStatusEnum.PENDING;
                }
            }
            item.setPlatformIdentityStatus(identityStatus.getCode());
            item.setPlatformIdentityStatusText(identityStatus.getLabel());
            String cannotSelectReason = resolveRosterCandidateReason(profile, identityStatus);
            item.setCanSelect(cannotSelectReason == null);
            item.setCannotSelectReason(cannotSelectReason);
            items.add(item);
        }
        return items;
    }

    private String resolveRosterCandidateReason(BasketPlayerProfile profile, PlatformIdentityStatusEnum identityStatus) {
        if (profile == null) {
            return "球员档案不可用";
        }
        if (!Objects.equals(PlayerCertStatusEnum.APPROVED.getCode(), profile.getCertificationStatus())) {
            return "球员未完成认证";
        }
        if (identityStatus != PlatformIdentityStatusEnum.ACTIVE) {
            return "该成员未绑定或未通过当前平台游戏 ID，不能加入本平台阵容";
        }
        return null;
    }

    @Override
    public AvailableSeasonPageResp pageAvailableSeasons(String teamId, Integer currentPage, Integer pageSize) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        Date now = new Date();

        BasketTeam team;
        try {
            captainPermissionService.requireTeamMember(memberId, teamPk);
            team = teamDao.selectById(teamPk);
        } catch (ServiceException ex) {
            AvailableSeasonPageResp emptyPage = buildAvailablePage(Collections.emptyList(), currentPage, pageSize);
            emptyPage.setEmptyReason(ex.getMessage());
            return emptyPage;
        }
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            AvailableSeasonPageResp emptyPage = buildAvailablePage(Collections.emptyList(), currentPage, pageSize);
            emptyPage.setEmptyReason("球队不存在");
            return emptyPage;
        }

        boolean isTeamCaptain = Objects.equals(team.getCaptainMemberId(), memberId);
        List<BasketCaptainQualification> eventGrants = loadRuntimeValidRegistrationGrants(memberId, teamPk, now);
        if (!isTeamCaptain && eventGrants.isEmpty()) {
            AvailableSeasonPageResp emptyPage = buildAvailablePage(Collections.emptyList(), currentPage, pageSize);
            emptyPage.setEmptyReason("当前用户不是球队队长，不能代表球队报名赛事");
            return emptyPage;
        }

        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            AvailableSeasonPageResp emptyPage = buildAvailablePage(Collections.emptyList(), currentPage, pageSize);
            emptyPage.setEmptyReason("当前球队尚未认证，认证通过后可报名赛事");
            return emptyPage;
        }

        if (eventGrants.isEmpty()) {
            AvailableSeasonPageResp emptyPage = buildAvailablePage(Collections.emptyList(), currentPage, pageSize);
            emptyPage.setEmptyReason("当前暂无可报名赛事，请确认后台是否已授予当前赛事队长权限");
            return emptyPage;
        }

        List<AvailableSeasonItemResp> registerableItems = new ArrayList<>();
        Set<Long> processedSeasonIds = new HashSet<>();
        for (BasketCaptainQualification grant : eventGrants) {
            if (grant.getSeasonId() == null || processedSeasonIds.contains(grant.getSeasonId())) {
                continue;
            }
            if (grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), teamPk)) {
                continue;
            }
            processedSeasonIds.add(grant.getSeasonId());
            registerableItems.addAll(buildRegisterableItemsForGrantedSeason(team, teamPk, grant.getSeasonId(), now));
        }

        AvailableSeasonPageResp page = buildAvailablePage(registerableItems, currentPage, pageSize);
        if (registerableItems.isEmpty()) {
            page.setEmptyReason("暂无可报名赛事");
        }
        return page;
    }

    private List<AvailableSeasonItemResp> buildRegisterableItemsForGrantedSeason(BasketTeam team,
                                                                                  Long teamPk,
                                                                                  Long seasonPk,
                                                                                  Date now) {
        BasketTournamentSeason season = seasonDao.selectById(seasonPk);
        if (season == null || !NOT_DELETED.equals(season.getIsDelete())) {
            return Collections.emptyList();
        }
        BasketTournament tournament = tournamentDao.selectById(season.getTournamentId());
        if (tournament == null || !NOT_DELETED.equals(tournament.getIsDelete())) {
            return Collections.emptyList();
        }
        if (!Objects.equals(TournamentStatusEnum.ACTIVE.getCode(), tournament.getTournamentStatus())) {
            return Collections.emptyList();
        }
        Map<Long, BasketCompetitionSeason> competitionSeasonMap = loadCompetitionSeasonMap(Collections.singletonList(tournament));
        if (!isCompetitionSeasonValid(tournament, competitionSeasonMap)) {
            return Collections.emptyList();
        }

        String registrationStatus = resolveRegistrationStatus(season, tournament, now);
        if (!"OPEN".equals(registrationStatus)) {
            return Collections.emptyList();
        }
        if (Integer.valueOf(0).equals(season.getAllowTeamRegistration())) {
            return Collections.emptyList();
        }
        String quotaReason = resolveQuotaExceededReason(season, loadSeasonRegistrationCountMap(Collections.singletonList(season)));
        if (quotaReason != null) {
            return Collections.emptyList();
        }

        Set<String> registeredPlatforms = loadRegisteredPlatformCodes(teamPk, season.getId());
        if (!registeredPlatforms.isEmpty()) {
            return Collections.emptyList();
        }
        String platformCode = eventPlatformSupport.resolveEventPlatformCode(season.getId());
        if (!StringUtils.hasText(platformCode)) {
            return Collections.emptyList();
        }
        AvailableSeasonItemResp resp = buildAvailableSeasonItem(
                season, tournament, team, null, registrationStatus, competitionSeasonMap);
        resp.setPlatformCode(platformCode);
        resp.setPlatformName(GameTerminalSupport.formatDisplayName(platformCode));
        resp.setCanRegister(true);
        return List.of(resp);
    }

    @Override
    public MyEventsContextResp getMyEventsContext() {
        Long memberId = UserContext.getRequiredUserId();
        Date now = new Date();
        MyEventsContextResp resp = new MyEventsContextResp();
        resp.setHasTeam(false);
        resp.setTeamRole("NONE");
        resp.setIsTeamCaptain(false);
        resp.setHasActiveCaptainGrant(false);
        resp.setCanManageTeamRegistration(false);
        resp.setCurrentRoleMode("NO_TEAM");

        BasketTeamMember membership = teamMemberDao.selectOne(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamMember::getCreateTime)
                .last("limit 1"));
        if (membership == null) {
            BasketCaptainQualification grant = captainGrantSupport.findCurrentGrant(memberId);
            String grantInvalidReason = resolveCaptainGrantInvalidReason(memberId, grant, now);
            boolean hasActiveGrant = grant != null && grantInvalidReason == null;
            if (grant != null) {
                fillGrantSummary(resp, grant, hasActiveGrant, grantInvalidReason);
            }
            resp.setMode(resp.getCurrentRoleMode());
            resp.setActiveCaptainGrantSeasonIds(buildActiveCaptainGrantSeasonIds(memberId, null, now));
            return resp;
        }

        BasketTeam team = teamDao.selectById(membership.getTeamId());
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            BasketCaptainQualification grant = captainGrantSupport.findCurrentGrant(memberId);
            String grantInvalidReason = resolveCaptainGrantInvalidReason(memberId, grant, now);
            boolean hasActiveGrant = grant != null && grantInvalidReason == null;
            if (grant != null) {
                fillGrantSummary(resp, grant, hasActiveGrant, grantInvalidReason);
            }
            resp.setMode(resp.getCurrentRoleMode());
            resp.setActiveCaptainGrantSeasonIds(buildActiveCaptainGrantSeasonIds(memberId, null, now));
            return resp;
        }

        boolean isTeamCaptain = memberId.equals(team.getCaptainMemberId());
        List<BasketCaptainQualification> runtimeValidGrants = loadRuntimeValidRegistrationGrants(memberId, team.getId(), now);
        boolean hasActiveGrant = !runtimeValidGrants.isEmpty();

        resp.setHasTeam(true);
        resp.setTeamId(toStringId(team.getId()));
        resp.setTeamName(team.getTeamName());
        resp.setTeamCertificationStatus(team.getAuditStatus());
        resp.setGameTerminal(team.getGameTerminal());
        resp.setTeamRole(isTeamCaptain ? "CAPTAIN" : "MEMBER");
        resp.setIsTeamCaptain(isTeamCaptain);
        resp.setHasActiveCaptainGrant(hasActiveGrant);
        resp.setCanManageTeamRegistration(isTeamCaptain);
        if (!runtimeValidGrants.isEmpty()) {
            fillGrantSummary(resp, runtimeValidGrants.get(0), true, null);
        } else {
            BasketCaptainQualification latestGrant = captainGrantSupport.findCurrentGrant(memberId);
            String grantInvalidReason = resolveCaptainGrantInvalidReason(memberId, latestGrant, now);
            fillGrantSummary(resp, latestGrant, false, grantInvalidReason);
        }

        if (isTeamCaptain) {
            resp.setCurrentRoleMode("TEAM_CAPTAIN");
        } else {
            resp.setCurrentRoleMode("TEAM_MEMBER");
        }
        resp.setMode(resp.getCurrentRoleMode());
        resp.setActiveCaptainGrantSeasonIds(buildActiveCaptainGrantSeasonIds(memberId, team.getId(), now));
        return resp;
    }

    private List<String> buildActiveCaptainGrantSeasonIds(Long memberId, Long teamId, Date now) {
        return loadRuntimeValidRegistrationGrants(memberId, teamId, now).stream()
                .map(BasketCaptainQualification::getSeasonId)
                .filter(Objects::nonNull)
                .distinct()
                .map(this::toStringId)
                .collect(Collectors.toList());
    }

    private List<BasketCaptainQualification> loadRuntimeValidRegistrationGrants(Long memberId, Long teamId, Date now) {
        if (memberId == null) {
            return Collections.emptyList();
        }
        List<BasketCaptainQualification> grants = captainGrantSupport.findActiveGrantsForMemberAndTeam(memberId, teamId);
        List<BasketCaptainQualification> validGrants = new ArrayList<>();
        for (BasketCaptainQualification grant : grants) {
            if (grant.getSeasonId() == null) {
                continue;
            }
            if (teamId != null && grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), teamId)) {
                continue;
            }
            if (resolveCaptainGrantInvalidReason(memberId, grant, now) == null) {
                validGrants.add(grant);
            }
        }
        return validGrants;
    }

    @Override
    public AvailableSeasonPageResp pageMyEventsSeasons(MyEventsSeasonPageQuery query) {
        MyEventsSeasonPageQuery safeQuery = query == null ? new MyEventsSeasonPageQuery() : query;
        Long memberId = UserContext.getRequiredUserId();
        Date now = new Date();
        MyEventsContextResp context = getMyEventsContext();

        BasketTeam team = null;
        if (Boolean.TRUE.equals(context.getHasTeam()) && StringUtils.hasText(context.getTeamId())) {
            team = teamDao.selectById(parseLongId(context.getTeamId(), "球队主键"));
        }
        boolean isTeamCaptain = Boolean.TRUE.equals(context.getIsTeamCaptain());

        List<BasketTournamentSeason> seasons = loadBrowsableSeasons();
        Map<Long, BasketTournament> tournamentMap = loadTournamentMap(seasons);
        Map<Long, BasketCompetitionSeason> competitionSeasonMap = loadCompetitionSeasonMap(tournamentMap.values());
        Map<Long, List<BasketTournamentRegistration>> teamRegistrationMap = team == null
                ? Collections.emptyMap()
                : loadTeamRegistrationListMap(team.getId(), seasons);
        Map<Long, Boolean> memberRosterMap = team == null
                ? Collections.emptyMap()
                : loadMemberRosterSelectionMap(memberId, teamRegistrationMap);

        List<AvailableSeasonItemResp> items = new ArrayList<>();
        for (BasketTournamentSeason season : seasons) {
            BasketTournament tournament = tournamentMap.get(season.getTournamentId());
            if (tournament == null || !Objects.equals(TournamentStatusEnum.ACTIVE.getCode(), tournament.getTournamentStatus())) {
                continue;
            }
            if (!Integer.valueOf(1).equals(tournament.getPublicStatus())) {
                continue;
            }
            if (!isCompetitionSeasonValid(tournament, competitionSeasonMap)) {
                continue;
            }

            String registrationStatus = resolveRegistrationStatus(season, tournament, now);
            List<BasketTournamentRegistration> registrations = teamRegistrationMap.getOrDefault(season.getId(), Collections.emptyList());
            BasketTournamentRegistration primaryRegistration = pickPrimaryRegistration(registrations);
            AvailableSeasonItemResp resp = buildAvailableSeasonItem(
                    season, tournament, team, primaryRegistration, registrationStatus, competitionSeasonMap);
            fillSeasonPlatformFields(resp, season.getId(), registrations, tournament);
            BasketCaptainQualification registrationGrant = resolveRegistrationGrantForEvent(
                    memberId, team == null ? null : team.getId(), season.getId(), now);
            fillMyEventsPresentationFields(resp, context, isTeamCaptain, season, tournament, team,
                    registrations, memberRosterMap.get(season.getId()), memberId, now,
                    registrationGrant,
                    resolveTeamCaptainGrantForEvent(team, season.getId(), now));
            if (matchesMyEventsFilters(resp, season, tournament, registrationStatus, safeQuery, now)) {
                items.add(resp);
            }
        }

        sortMyEventsItems(items, tournamentMap);

        int pageNum = resolvePageNum(safeQuery);
        int pageSize = resolvePageSize(safeQuery);
        AvailableSeasonPageResp page = buildAvailablePage(items, pageNum, pageSize);
        if (items.isEmpty()) {
            page.setEmptyReason(resolveMyEventsEmptyReason(context));
        }
        return page;
    }

    @Override
    public List<CompetitionSeasonOptionResp> listMyEventsCompetitionSeasonOptions() {
        List<BasketCompetitionSeason> seasons = competitionSeasonDao.selectList(new LambdaQueryWrapper<BasketCompetitionSeason>()
                .eq(BasketCompetitionSeason::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketCompetitionSeason::getSeasonYear)
                .orderByDesc(BasketCompetitionSeason::getId));
        List<CompetitionSeasonOptionResp> options = new ArrayList<>();
        for (BasketCompetitionSeason season : seasons) {
            CompetitionSeasonOptionResp option = new CompetitionSeasonOptionResp();
            option.setSeasonId(season.getId());
            option.setSeasonYear(season.getSeasonYear());
            option.setSeasonName(formatCompetitionSeasonDisplayName(season));
            options.add(option);
        }
        return options;
    }

    private void fillGrantSummary(MyEventsContextResp resp,
                                  BasketCaptainQualification grant,
                                  boolean hasActiveGrant,
                                  String grantInvalidReason) {
        if (grant == null) {
            resp.setCaptainGrantStatus(hasActiveGrant ? "ACTIVE" : "NONE");
            resp.setCaptainGrantStatusText(hasActiveGrant ? "有效" : "暂无授权");
            return;
        }
        resp.setCaptainGrantSeasonId(toStringId(grant.getSeasonId()));
        resp.setCaptainGrantExpireTime(grant.getValidTo());
        CaptainQualificationStatusEnum statusEnum = CaptainQualificationStatusEnum.fromCode(grant.getQualificationStatus());
        resp.setCaptainGrantStatus(statusEnum == null ? "UNKNOWN" : statusEnum.name());
        if (hasActiveGrant) {
            resp.setCaptainGrantStatusText("有效");
        } else if (grantInvalidReason != null) {
            resp.setCaptainGrantStatusText(grantInvalidReason);
        } else {
            resp.setCaptainGrantStatusText("无效");
        }
    }

    private List<BasketTournamentSeason> loadBrowsableSeasons() {
        return seasonDao.selectList(new LambdaQueryWrapper<BasketTournamentSeason>()
                .eq(BasketTournamentSeason::getIsDelete, NOT_DELETED)
                .and(w -> w.eq(BasketTournamentSeason::getAllowTeamRegistration, ENABLED)
                        .or().isNull(BasketTournamentSeason::getAllowTeamRegistration))
                .notIn(BasketTournamentSeason::getSeasonStatus,
                        TournamentSeasonStatusEnum.FINISHED.getCode(),
                        TournamentSeasonStatusEnum.CLOSED.getCode(),
                        TournamentSeasonStatusEnum.CANCELLED.getCode())
                .orderByDesc(BasketTournamentSeason::getRegistrationStartTime));
    }

    private Map<Long, List<BasketTournamentRegistration>> loadTeamRegistrationListMap(Long teamId,
                                                                                       List<BasketTournamentSeason> seasons) {
        if (teamId == null || CollectionUtils.isEmpty(seasons)) {
            return Collections.emptyMap();
        }
        Set<Long> seasonIds = seasons.stream().map(BasketTournamentSeason::getId).collect(Collectors.toSet());
        Map<Long, List<BasketTournamentRegistration>> map = new HashMap<>();
        for (BasketTournamentRegistration registration : registrationDao.selectList(new LambdaQueryWrapper<BasketTournamentRegistration>()
                .eq(BasketTournamentRegistration::getTeamId, teamId)
                .in(BasketTournamentRegistration::getSeasonId, seasonIds)
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED))) {
            map.computeIfAbsent(registration.getSeasonId(), key -> new ArrayList<>()).add(registration);
        }
        return map;
    }

    private BasketTournamentRegistration pickPrimaryRegistration(List<BasketTournamentRegistration> registrations) {
        if (CollectionUtils.isEmpty(registrations)) {
            return null;
        }
        for (BasketTournamentRegistration registration : registrations) {
            if (isBlockingRegistration(registration.getRegistrationStatus())) {
                return registration;
            }
        }
        return registrations.get(0);
    }

    private Map<Long, Boolean> loadMemberRosterSelectionMap(Long memberId,
                                                            Map<Long, List<BasketTournamentRegistration>> teamRegistrationMap) {
        if (memberId == null || teamRegistrationMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> registrationIds = teamRegistrationMap.values().stream()
                .flatMap(List::stream)
                .map(BasketTournamentRegistration::getId)
                .collect(Collectors.toSet());
        if (registrationIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> selectedRegistrationIds = registrationPlayerDao.selectList(new LambdaQueryWrapper<BasketTournamentRegistrationPlayer>()
                        .in(BasketTournamentRegistrationPlayer::getRegistrationId, registrationIds)
                        .eq(BasketTournamentRegistrationPlayer::getMemberId, memberId)
                        .eq(BasketTournamentRegistrationPlayer::getIsDelete, NOT_DELETED))
                .stream()
                .map(BasketTournamentRegistrationPlayer::getRegistrationId)
                .collect(Collectors.toSet());
        Map<Long, Boolean> map = new HashMap<>();
        for (Map.Entry<Long, List<BasketTournamentRegistration>> entry : teamRegistrationMap.entrySet()) {
            boolean selected = entry.getValue().stream().anyMatch(item -> selectedRegistrationIds.contains(item.getId()));
            map.put(entry.getKey(), selected);
        }
        return map;
    }

    private void fillSeasonPlatformFields(AvailableSeasonItemResp resp,
                                          Long seasonId,
                                          List<BasketTournamentRegistration> registrations,
                                          BasketTournament tournament) {
        List<SeasonPlatformItemResp> openPlatforms = eventPlatformSupport.listSeasonPlatforms(seasonId);
        List<String> opened = new ArrayList<>();
        List<String> registered = new ArrayList<>();
        List<String> available = new ArrayList<>();
        Set<String> registeredSet = new HashSet<>();
        for (BasketTournamentRegistration registration : registrations) {
            if (isBlockingRegistration(registration.getRegistrationStatus())
                    && StringUtils.hasText(registration.getPlatformCode())) {
                registeredSet.add(registration.getPlatformCode().trim().toUpperCase());
            }
        }
        for (SeasonPlatformItemResp platform : openPlatforms) {
            if (!Boolean.TRUE.equals(platform.getOpen()) || !StringUtils.hasText(platform.getPlatformCode())) {
                continue;
            }
            String code = platform.getPlatformCode().trim().toUpperCase();
            opened.add(code);
            if (registeredSet.contains(code)) {
                registered.add(code);
            } else {
                available.add(code);
            }
        }
        resp.setOpenedPlatforms(opened);
        resp.setRegisteredPlatforms(registered);
        resp.setAvailableRegisterPlatforms(available);

        String tournamentTerminal = tournament == null ? null : GameTerminalSupport.normalize(tournament.getGameTerminal());
        if (StringUtils.hasText(tournamentTerminal)) {
            if (opened.isEmpty()) {
                opened.add(tournamentTerminal);
                if (!registeredSet.contains(tournamentTerminal)) {
                    available.add(tournamentTerminal);
                }
                resp.setOpenedPlatforms(opened);
                resp.setAvailableRegisterPlatforms(available);
            }
            resp.setPlatformCode(tournamentTerminal);
            resp.setPlatformName(GameTerminalSupport.formatDisplayName(tournamentTerminal));
            return;
        }
        if (!opened.isEmpty()) {
            resp.setPlatformCode(String.join("/", opened));
            resp.setPlatformName(opened.stream()
                    .map(GameTerminalSupport::formatDisplayName)
                    .collect(Collectors.joining(" / ")));
        }
    }

    private BasketCaptainQualification resolveRegistrationGrantForEvent(Long memberId,
                                                                        Long teamId,
                                                                        Long seasonId,
                                                                        Date now) {
        if (memberId == null || teamId == null || seasonId == null) {
            return null;
        }
        BasketCaptainQualification grant = captainGrantSupport.findActiveGrantForEvent(memberId, teamId, seasonId);
        if (grant == null) {
            return null;
        }
        if (resolveCaptainGrantInvalidReason(memberId, grant, now) != null) {
            return null;
        }
        if (grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), teamId)) {
            return null;
        }
        if (!Objects.equals(grant.getSeasonId(), seasonId)) {
            return null;
        }
        return grant;
    }

    private BasketCaptainQualification resolveTeamCaptainGrantForEvent(BasketTeam team, Long seasonId, Date now) {
        if (team == null || seasonId == null || team.getCaptainMemberId() == null) {
            return null;
        }
        BasketCaptainQualification grant = captainGrantSupport.findActiveGrantForEvent(
                team.getCaptainMemberId(), team.getId(), seasonId);
        if (grant == null) {
            return null;
        }
        if (resolveCaptainGrantInvalidReason(team.getCaptainMemberId(), grant, now) != null) {
            return null;
        }
        return grant;
    }

    private void fillMyEventsPresentationFields(AvailableSeasonItemResp resp,
                                                MyEventsContextResp context,
                                                boolean canManageTeam,
                                                BasketTournamentSeason season,
                                                BasketTournament tournament,
                                                BasketTeam team,
                                                List<BasketTournamentRegistration> registrations,
                                                Boolean rosterSelected,
                                                Long memberId,
                                                Date now,
                                                BasketCaptainQualification registrationGrant,
                                                BasketCaptainQualification captainDisplayGrant) {
        resp.setMatchStartTime(resolveMatchStartTime(season, tournament));
        resp.setMatchEndTime(resolveMatchEndTime(season, tournament));
        resp.setSignupStartTime(resolveSignupStartTime(season, tournament));
        resp.setSignupEndTime(resolveSignupEndTime(season, tournament));
        resp.setUserTeamRole(context.getTeamRole());
        resp.setUserRoleInTeam(context.getTeamRole());
        resp.setIsCaptain(canManageTeam);
        resp.setTeamRegistrationStatus(resolveTeamRegistrationStatusCode(registrations));
        resp.setTeamRegistrationStatusText(resolveTeamRegistrationStatusText(registrations));
        resp.setTeamRegistrationStatusName(resp.getTeamRegistrationStatusText());
        resp.setUserRosterStatus(resolveUserRosterStatus(rosterSelected, registrations));
        resp.setMyRosterStatus(resp.getUserRosterStatus());
        resp.setUserRosterStatusText(resolveUserRosterStatusText(resp.getUserRosterStatus()));
        resp.setMyRosterStatusName(resp.getUserRosterStatusText());

        boolean grantCoversEvent = registrationGrant != null;
        resp.setHasActiveCaptainGrantForEvent(grantCoversEvent);
        resp.setHasCaptainGrant(grantCoversEvent);
        resp.setCaptainGrantStatus(grantCoversEvent ? "ACTIVE" : "NONE");
        String eventStatusCode = resolveEventDisplayStatusCode(season, tournament, resp.getRegistrationStatus(), now);
        resp.setEventStatus(eventStatusCode);
        resp.setEventStatusName(registrationStatusLabel(eventStatusCode));

        BasketTournamentRegistration primaryRegistration = pickPrimaryRegistration(registrations);
        boolean hasTeamRegistration = primaryRegistration != null
                && isBlockingRegistration(primaryRegistration.getRegistrationStatus());

        if (!Boolean.TRUE.equals(context.getHasTeam())) {
            resp.setUserMembershipStatusText("未加入球队");
            applyMyEventsAction(resp, false, "JOIN_TEAM", "先加入球队",
                    "你当前还未加入球队，请先加入球队后，由队长统一报名赛事。");
            syncUnifiedApiFields(resp);
            return;
        }

        if (!canManageTeam) {
            resp.setCaptainGrantStatus("CAPTAIN_ONLY");
            resp.setCaptainGrantStatusText("仅队长可操作");
            fillMemberRemindFields(resp, team, season, tournament, primaryRegistration, memberId, now);
            fillMemberAction(resp, primaryRegistration, rosterSelected);
            syncUnifiedApiFields(resp);
            return;
        }

        fillCaptainGrantStatusText(resp, registrationGrant, grantCoversEvent, now);
        if (!grantCoversEvent) {
            resp.setCaptainGrantStatusText("暂无该赛事报名资格");
        }

        if (hasTeamRegistration) {
            fillCaptainRegistrationAction(resp, primaryRegistration);
            syncUnifiedApiFields(resp);
            return;
        }

        if (!grantCoversEvent) {
            applyMyEventsAction(resp, false, "NO_GRANT", "暂无该赛事报名资格",
                    "当前球队暂未获得该赛事报名资格，请联系管理员确认缴费和授权。");
            syncUnifiedApiFields(resp);
            return;
        }

        String registrationStatus = resp.getRegistrationStatus();
        if ("NOT_STARTED".equals(registrationStatus)) {
            applyMyEventsAction(resp, false, "NOT_STARTED", "报名未开始", "当前赛事暂未开放报名。");
            syncUnifiedApiFields(resp);
            return;
        }
        if ("CLOSED".equals(registrationStatus) || "ENDED".equals(registrationStatus)) {
            applyMyEventsAction(resp, false, "CLOSED", "报名已结束", "当前赛事报名已结束。");
            syncUnifiedApiFields(resp);
            return;
        }
        if (!Objects.equals(TeamAuditStatusEnum.APPROVED.getCode(), team.getAuditStatus())) {
            applyMyEventsAction(resp, false, "DISABLED", "暂不可报名", "当前球队尚未认证，认证通过后可报名赛事");
            syncUnifiedApiFields(resp);
            return;
        }
        if (registrationGrant.getTeamId() != null && !Objects.equals(registrationGrant.getTeamId(), team.getId())) {
            applyMyEventsAction(resp, false, "NO_GRANT", "暂无该赛事报名资格",
                    "当前球队暂未获得该赛事报名资格，请联系管理员确认缴费和授权。");
            syncUnifiedApiFields(resp);
            return;
        }

        List<String> availablePlatforms = resp.getAvailableRegisterPlatforms();
        if (CollectionUtils.isEmpty(availablePlatforms)) {
            applyMyEventsAction(resp, false, "DISABLED", "暂不可报名",
                    CollectionUtils.isEmpty(resp.getOpenedPlatforms()) ? "当前赛事暂未配置开放参赛端" : "当前暂无可报名参赛端");
            syncUnifiedApiFields(resp);
            return;
        }

        applyMyEventsAction(resp, true, "REGISTER", "立即报名", null);
        syncUnifiedApiFields(resp);
    }

    private void fillMemberAction(AvailableSeasonItemResp resp,
                                  BasketTournamentRegistration primaryRegistration,
                                  Boolean rosterSelected) {
        if (primaryRegistration == null || !isBlockingRegistration(primaryRegistration.getRegistrationStatus())) {
            applyMyEventsAction(resp, false, "CONTACT_CAPTAIN", "联系队长报名",
                    "当前用户不是球队队长，请联系队长报名。");
            return;
        }
        Integer registrationStatus = primaryRegistration.getRegistrationStatus();
        if (Objects.equals(TournamentRegistrationStatusEnum.APPROVED.getCode(), registrationStatus)) {
            if (Boolean.TRUE.equals(rosterSelected)) {
                applyMyEventsAction(resp, false, "VIEW_ROSTER", "已入选阵容", null);
            } else {
                applyMyEventsAction(resp, false, "ROSTER_NOT_SELECTED", "未入选阵容", null);
            }
            return;
        }
        if (Objects.equals(TournamentRegistrationStatusEnum.REJECTED.getCode(), registrationStatus)) {
            applyMyEventsAction(resp, false, "VIEW_REGISTRATION", "查看驳回原因", null);
            return;
        }
        applyMyEventsAction(resp, false, "VIEW_REGISTRATION", "查看报名状态", null);
    }

    private void fillCaptainRegistrationAction(AvailableSeasonItemResp resp,
                                               BasketTournamentRegistration primaryRegistration) {
        Integer registrationStatus = primaryRegistration.getRegistrationStatus();
        if (Objects.equals(TournamentRegistrationStatusEnum.REJECTED.getCode(), registrationStatus)) {
            applyMyEventsAction(resp, true, "RE_REGISTER", "重新报名", null);
            return;
        }
        if (Objects.equals(TournamentRegistrationStatusEnum.APPROVED.getCode(), registrationStatus)) {
            applyMyEventsAction(resp, false, "VIEW_ROSTER", "查看阵容", null);
            return;
        }
        if (Objects.equals(TournamentRegistrationStatusEnum.DRAFT.getCode(), registrationStatus)) {
            applyMyEventsAction(resp, true, "MANAGE_REGISTRATION", "继续报名", null);
            return;
        }
        applyMyEventsAction(resp, false, "VIEW_REGISTRATION", "查看状态", null);
    }

    private void syncUnifiedApiFields(AvailableSeasonItemResp resp) {
        resp.setOperationalSeasonId(resp.getSeasonId());
        resp.setUserTeamId(resp.getTeamId());
        resp.setUserTeamName(resp.getTeamName());
        resp.setOperationType(resp.getActionCode());
        if (resp.getSignupStartTime() == null) {
            resp.setSignupStartTime(resp.getRegistrationStartTime());
        }
        if (resp.getSignupEndTime() == null) {
            resp.setSignupEndTime(resp.getRegistrationEndTime());
        }
        if (!StringUtils.hasText(resp.getTeamRegistrationStatusName())) {
            resp.setTeamRegistrationStatusName(resp.getTeamRegistrationStatusText());
        }
        if (!StringUtils.hasText(resp.getMyRosterStatus())) {
            resp.setMyRosterStatus(resp.getUserRosterStatus());
        }
        if (!StringUtils.hasText(resp.getMyRosterStatusName())) {
            resp.setMyRosterStatusName(resp.getUserRosterStatusText());
        }
        if (resp.getHasCaptainGrant() == null) {
            resp.setHasCaptainGrant(resp.getHasActiveCaptainGrantForEvent());
        }
    }

    private void fillCaptainGrantStatusText(AvailableSeasonItemResp resp,
                                            BasketCaptainQualification displayGrant,
                                            boolean grantCoversEvent,
                                            Date now) {
        if (displayGrant == null) {
            resp.setCaptainGrantStatusText("队长暂无授权");
            return;
        }
        resp.setCaptainGrantExpireTime(displayGrant.getValidTo());
        String invalidReason = resolveCaptainGrantInvalidReason(displayGrant.getMemberId(), displayGrant, now);
        if (grantCoversEvent) {
            resp.setCaptainGrantStatusText("队长具备授权");
        } else if (invalidReason != null && displayGrant.getValidTo() != null && now.after(displayGrant.getValidTo())) {
            resp.setCaptainGrantStatusText("授权已过期");
        } else {
            CaptainQualificationStatusEnum statusEnum = CaptainQualificationStatusEnum.fromCode(displayGrant.getQualificationStatus());
            resp.setCaptainGrantStatusText(statusEnum == null ? "队长暂无授权" : statusEnum.getDescription());
        }
    }

    private boolean hasEditableRegistration(List<BasketTournamentRegistration> registrations) {
        for (BasketTournamentRegistration registration : registrations) {
            Integer status = registration.getRegistrationStatus();
            if (Objects.equals(TournamentRegistrationStatusEnum.DRAFT.getCode(), status)
                    || Objects.equals(TournamentRegistrationStatusEnum.SUBMITTED.getCode(), status)
                    || Objects.equals(TournamentRegistrationStatusEnum.REVIEWING.getCode(), status)) {
                return true;
            }
        }
        return false;
    }

    private String resolveTeamRegistrationStatusText(List<BasketTournamentRegistration> registrations) {
        if (CollectionUtils.isEmpty(registrations)) {
            return "未报名";
        }
        BasketTournamentRegistration primary = pickPrimaryRegistration(registrations);
        if (primary == null) {
            return "未报名";
        }
        return TournamentRegistrationStatusEnum.fromCode(primary.getRegistrationStatus()).getDescription();
    }

    private String resolveTeamRegistrationStatusCode(List<BasketTournamentRegistration> registrations) {
        if (CollectionUtils.isEmpty(registrations)) {
            return "NOT_REGISTERED";
        }
        BasketTournamentRegistration primary = pickPrimaryRegistration(registrations);
        if (primary == null || primary.getRegistrationStatus() == null) {
            return "NOT_REGISTERED";
        }
        return String.valueOf(primary.getRegistrationStatus());
    }

    private String resolveUserRosterStatus(Boolean rosterSelected, List<BasketTournamentRegistration> registrations) {
        if (CollectionUtils.isEmpty(registrations)) {
            return "NO_ROSTER";
        }
        if (Boolean.TRUE.equals(rosterSelected)) {
            return "SELECTED";
        }
        return "NOT_SELECTED";
    }

    private String resolveUserRosterStatusText(String rosterStatus) {
        if ("SELECTED".equals(rosterStatus)) {
            return "已入选阵容";
        }
        if ("NOT_SELECTED".equals(rosterStatus)) {
            return "未入选阵容";
        }
        return "暂无阵容";
    }

    private void applyMyEventsAction(AvailableSeasonItemResp resp,
                                     boolean canRegister,
                                     String actionCode,
                                     String actionText,
                                     String disabledReason) {
        resp.setCanRegister(canRegister);
        resp.setActionCode(actionCode);
        resp.setActionText(actionText);
        resp.setOperationType(actionCode);
        resp.setCannotRegisterReason(disabledReason);
        resp.setDisabledReason(disabledReason);
    }

    private String resolveMyEventsEmptyReason(MyEventsContextResp context) {
        if (!Boolean.TRUE.equals(context.getHasTeam())) {
            return "当前暂无公开赛事";
        }
        return "当前暂无符合条件的公开赛事";
    }

    private boolean hasPendingReviewRegistration(List<BasketTournamentRegistration> registrations) {
        if (CollectionUtils.isEmpty(registrations)) {
            return false;
        }
        for (BasketTournamentRegistration registration : registrations) {
            Integer status = registration.getRegistrationStatus();
            if (Objects.equals(TournamentRegistrationStatusEnum.SUBMITTED.getCode(), status)
                    || Objects.equals(TournamentRegistrationStatusEnum.REVIEWING.getCode(), status)) {
                return true;
            }
        }
        return false;
    }

    private String registrationStatusLabel(String registrationStatus) {
        if (!StringUtils.hasText(registrationStatus)) {
            return "未知";
        }
        return switch (registrationStatus) {
            case "NOT_STARTED" -> "报名未开始";
            case "OPEN" -> "报名中";
            case "CLOSED" -> "报名已结束";
            case "IN_PROGRESS" -> "进行中";
            case "ENDED" -> "已结束";
            default -> registrationStatus;
        };
    }

    private String resolveEventDisplayStatusCode(BasketTournamentSeason season,
                                                 BasketTournament tournament,
                                                 String registrationStatus,
                                                 Date now) {
        if (Objects.equals(TournamentSeasonStatusEnum.IN_PROGRESS.getCode(), season.getSeasonStatus())) {
            return "IN_PROGRESS";
        }
        if (Objects.equals(TournamentSeasonStatusEnum.FINISHED.getCode(), season.getSeasonStatus())
                || Objects.equals(TournamentStatusEnum.ENDED.getCode(), tournament.getTournamentStatus())) {
            return "ENDED";
        }
        if (season.getSeasonStartTime() != null && season.getSeasonEndTime() != null
                && !now.before(season.getSeasonStartTime()) && !now.after(season.getSeasonEndTime())) {
            return "IN_PROGRESS";
        }
        return registrationStatus;
    }

    private boolean matchesMyEventsFilters(AvailableSeasonItemResp resp,
                                           BasketTournamentSeason season,
                                           BasketTournament tournament,
                                           String registrationStatus,
                                           MyEventsSeasonPageQuery query,
                                           Date now) {
        if (StringUtils.hasText(query.getEventName())) {
            String keyword = query.getEventName().trim().toLowerCase();
            String tournamentName = resp.getTournamentName() == null ? "" : resp.getTournamentName().toLowerCase();
            if (!tournamentName.contains(keyword)) {
                return false;
            }
        }
        if (query.getSeasonId() != null) {
            if (!Objects.equals(query.getSeasonId(), resp.getCompetitionSeasonId())) {
                return false;
            }
        }
        String platformFilter = resolvePlatformFilter(query.getPlatformCode());
        if (StringUtils.hasText(platformFilter) && !matchesPlatformFilter(tournament, resp, platformFilter)) {
            return false;
        }
        if (StringUtils.hasText(query.getStatus())) {
            String eventStatusCode = resolveEventDisplayStatusCode(season, tournament, registrationStatus, now);
            if (!query.getStatus().trim().equalsIgnoreCase(eventStatusCode)) {
                return false;
            }
        }
        if (query.getStartTime() != null) {
            Date signupEnd = resp.getSignupEndTime() != null ? resp.getSignupEndTime() : resp.getRegistrationEndTime();
            if (signupEnd == null || signupEnd.before(query.getStartTime())) {
                return false;
            }
        }
        if (query.getEndTime() != null) {
            Date signupStart = resp.getSignupStartTime() != null ? resp.getSignupStartTime() : resp.getRegistrationStartTime();
            if (signupStart == null || signupStart.after(query.getEndTime())) {
                return false;
            }
        }
        return true;
    }

    private void sortMyEventsItems(List<AvailableSeasonItemResp> items, Map<Long, BasketTournament> tournamentMap) {
        items.sort((left, right) -> {
            int statusCompare = Integer.compare(
                    myEventsStatusSortPriority(left.getEventStatus()),
                    myEventsStatusSortPriority(right.getEventStatus()));
            if (statusCompare != 0) {
                return statusCompare;
            }
            Date leftTime = resolveMyEventsSortTime(left, tournamentMap);
            Date rightTime = resolveMyEventsSortTime(right, tournamentMap);
            if (leftTime != null && rightTime != null) {
                int timeCompare = rightTime.compareTo(leftTime);
                if (timeCompare != 0) {
                    return timeCompare;
                }
            } else if (leftTime != null) {
                return -1;
            } else if (rightTime != null) {
                return 1;
            }
            long leftId = parseLongIdOrDefault(left.getSeasonId(), 0L);
            long rightId = parseLongIdOrDefault(right.getSeasonId(), 0L);
            return Long.compare(rightId, leftId);
        });
    }

    private int myEventsStatusSortPriority(String eventStatus) {
        if (!StringUtils.hasText(eventStatus)) {
            return 99;
        }
        return switch (eventStatus.trim().toUpperCase()) {
            case "OPEN" -> 0;
            case "NOT_STARTED" -> 1;
            default -> 2;
        };
    }

    private Date resolveMyEventsSortTime(AvailableSeasonItemResp item, Map<Long, BasketTournament> tournamentMap) {
        Long tournamentId = parseNullableLongId(item.getTournamentId());
        if (tournamentId != null) {
            BasketTournament tournament = tournamentMap.get(tournamentId);
            if (tournament != null) {
                if (tournament.getUpdateTime() != null) {
                    return tournament.getUpdateTime();
                }
                if (tournament.getCreateTime() != null) {
                    return tournament.getCreateTime();
                }
            }
        }
        return item.getSignupStartTime() != null ? item.getSignupStartTime() : item.getRegistrationStartTime();
    }

    private long parseLongIdOrDefault(String value, long defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private Long parseNullableLongId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Date resolveMatchStartTime(BasketTournamentSeason season, BasketTournament tournament) {
        if (season != null && season.getSeasonStartTime() != null) {
            return season.getSeasonStartTime();
        }
        return tournament == null ? null : tournament.getCompetitionStartTime();
    }

    private Date resolveMatchEndTime(BasketTournamentSeason season, BasketTournament tournament) {
        if (season != null && season.getSeasonEndTime() != null) {
            return season.getSeasonEndTime();
        }
        return tournament == null ? null : tournament.getCompetitionEndTime();
    }

    private Date resolveSignupStartTime(BasketTournamentSeason season, BasketTournament tournament) {
        if (season != null && season.getRegistrationStartTime() != null) {
            return season.getRegistrationStartTime();
        }
        return tournament == null ? null : tournament.getRegistrationStartTime();
    }

    private Date resolveSignupEndTime(BasketTournamentSeason season, BasketTournament tournament) {
        if (season != null && season.getRegistrationEndTime() != null) {
            return season.getRegistrationEndTime();
        }
        return tournament == null ? null : tournament.getRegistrationEndTime();
    }

    private String resolvePlatformFilter(String platformCode) {
        if (!StringUtils.hasText(platformCode)) {
            return null;
        }
        if ("ALL".equalsIgnoreCase(platformCode.trim())) {
            return null;
        }
        String normalized = GameTerminalSupport.normalize(platformCode);
        return StringUtils.hasText(normalized) ? normalized : platformCode.trim().toUpperCase();
    }

    private boolean matchesPlatformFilter(BasketTournament tournament,
                                          AvailableSeasonItemResp resp,
                                          String platformCode) {
        if (GameTerminalSupport.matches(tournament.getGameTerminal(), platformCode)) {
            return true;
        }
        if (!CollectionUtils.isEmpty(resp.getOpenedPlatforms())) {
            for (String opened : resp.getOpenedPlatforms()) {
                if (GameTerminalSupport.matches(opened, platformCode)) {
                    return true;
                }
            }
        }
        return GameTerminalSupport.matches(resp.getPlatformCode(), platformCode);
    }

    private int resolvePageNum(MyEventsSeasonPageQuery query) {
        if (query.getPageNum() != null && query.getPageNum() > 0) {
            return query.getPageNum();
        }
        if (query.getCurrentPage() != null && query.getCurrentPage() > 0) {
            return query.getCurrentPage();
        }
        return 1;
    }

    private int resolvePageSize(MyEventsSeasonPageQuery query) {
        if (query.getPageSize() != null && query.getPageSize() > 0) {
            return (int) Math.min(query.getPageSize(), 100);
        }
        return 5;
    }

    private String formatCompetitionSeasonDisplayName(BasketCompetitionSeason competitionSeason) {
        if (competitionSeason == null) {
            return null;
        }
        if (StringUtils.hasText(competitionSeason.getSeasonName())) {
            return competitionSeason.getSeasonName().contains("赛季")
                    ? competitionSeason.getSeasonName()
                    : competitionSeason.getSeasonName() + "赛季";
        }
        if (competitionSeason.getSeasonYear() != null) {
            return competitionSeason.getSeasonYear() + "赛季";
        }
        return null;
    }

    private String resolveEventTypeName(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return null;
        }
        return switch (eventType.trim().toUpperCase()) {
            case "REGULAR_SEASON", "REGULAR" -> "常规赛";
            case "PLAYOFFS", "PLAYOFF" -> "季后赛";
            case "ALL_STAR", "ALLSTAR" -> "全明星赛";
            case "LEAGUE" -> "联赛";
            default -> eventType;
        };
    }

    private BasketTeam requireTeamMemberOrCaptainForRegistration(Long memberId, Long teamPk, Long seasonPk) {
        BasketTeam team = teamDao.selectById(teamPk);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        try {
            captainPermissionService.requireTeamMember(memberId, teamPk);
        } catch (ServiceException ex) {
            throw new ServiceException("当前用户不是球队队长，不能代表球队报名赛事", 403);
        }
        if (Objects.equals(team.getCaptainMemberId(), memberId)) {
            return team;
        }
        BasketCaptainQualification grant = captainGrantSupport.findActiveGrantForEvent(memberId, teamPk, seasonPk);
        if (grant != null && resolveCaptainGrantInvalidReason(memberId, grant, new Date()) == null) {
            captainGrantSupport.assertCanManageTeam(grant, team, memberId);
            if (grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), teamPk)) {
                throw new ServiceException("当前用户不是球队队长，不能代表球队报名赛事", 403);
            }
            return team;
        }
        throw new ServiceException("当前用户不是球队队长，不能代表球队报名赛事", 403);
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
        if (!captainPermissionService.hasCurrentTeamMembership(memberId)) {
            throw new ServiceException("你当前还未加入球队，请先加入球队后，由队长统一报名赛事", 403);
        }
        BasketTeam team = requireTeamMemberOrCaptainForRegistration(memberId, teamPk, seasonPk);
        BasketCaptainQualification grant = captainPermissionService.requireValidQualificationForEvent(memberId, teamPk, seasonPk);
        captainGrantSupport.assertGrantBoundToTeam(grant, teamPk);
        BasketTournamentSeason season = requireOpenSeason(seasonPk);
        BasketTournament tournament = tournamentDao.selectById(season.getTournamentId());
        assertTeamRegistrationEligible(team, tournament);

        String platformCode = eventPlatformSupport.requireOpenPlatform(seasonPk, req.getPlatformCode());
        assertNoExistingRegistration(teamPk, seasonPk);

        BasketTournamentRegistration registration = new BasketTournamentRegistration();
        registration.setRegistrationNo("RG" + IdUtil.getSnowflakeNextIdStr());
        registration.setSeasonId(seasonPk);
        registration.setTeamId(teamPk);
        registration.setPlatformCode(platformCode);
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
        assertRegistrationEditable(registration);

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
        assertRegistrationEditable(registration);

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
            throw new ServiceException("球队已报名该赛事");
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
    }

    private String resolveCaptainGrantInvalidReason(Long memberId, BasketCaptainQualification grant, Date now) {
        if (grant == null) {
            return "当前暂无有效队长权限，暂不能报名赛事";
        }
        Integer playerCertStatus = null;
        try {
            playerCertStatus = captainPermissionService.requireCertifiedPlayer(memberId).getCertificationStatus();
        } catch (ServiceException ex) {
            return "当前暂无有效队长权限，暂不能报名赛事";
        }
        Integer seasonStatus = null;
        if (grant.getSeasonId() != null) {
            BasketTournamentSeason season = seasonDao.selectById(grant.getSeasonId());
            if (season != null) {
                seasonStatus = season.getSeasonStatus();
            }
        }
        String invalidReason = CaptainQualificationValiditySupport.resolveInvalidReason(
                grant, playerCertStatus, now, seasonStatus);
        if (invalidReason == null) {
            return null;
        }
        if (CaptainGrantSupport.NO_ACTIVE_GRANT_MESSAGE.equals(invalidReason)) {
            return "当前暂无有效队长权限，暂不能报名赛事";
        }
        return invalidReason;
    }

    private Set<String> loadRegisteredPlatformCodes(Long teamId, Long seasonId) {
        Set<String> registered = new HashSet<>();
        if (teamId == null || seasonId == null) {
            return registered;
        }
        for (BasketTournamentRegistration registration : registrationDao.selectList(new LambdaQueryWrapper<BasketTournamentRegistration>()
                .eq(BasketTournamentRegistration::getTeamId, teamId)
                .eq(BasketTournamentRegistration::getSeasonId, seasonId)
                .eq(BasketTournamentRegistration::getIsDelete, NOT_DELETED))) {
            if (!isBlockingRegistration(registration.getRegistrationStatus())) {
                continue;
            }
            if (StringUtils.hasText(registration.getPlatformCode())) {
                registered.add(registration.getPlatformCode().trim().toUpperCase());
            }
        }
        return registered;
    }

    private AvailableSeasonItemResp buildAvailableSeasonItem(BasketTournamentSeason season,
                                                             BasketTournament tournament,
                                                             BasketTeam team,
                                                             BasketTournamentRegistration existing,
                                                             String registrationStatus,
                                                             Map<Long, BasketCompetitionSeason> competitionSeasonMap) {
        AvailableSeasonItemResp resp = new AvailableSeasonItemResp();
        resp.setSeasonId(toStringId(season.getId()));
        resp.setOperationalSeasonId(toStringId(season.getId()));
        resp.setSeasonNo(season.getSeasonNo());
        resp.setOperationalSeasonName(season.getSeasonName());
        resp.setCompetitionSeasonId(tournament.getCompetitionSeasonId());
        BasketCompetitionSeason competitionSeason = null;
        if (tournament.getCompetitionSeasonId() != null) {
            competitionSeason = competitionSeasonMap.get(tournament.getCompetitionSeasonId());
            if (competitionSeason != null) {
                String displaySeasonName = formatCompetitionSeasonDisplayName(competitionSeason);
                resp.setCompetitionSeasonName(displaySeasonName);
                resp.setSeasonName(displaySeasonName);
            }
        }
        if (!StringUtils.hasText(resp.getSeasonName())) {
            resp.setSeasonName(season.getSeasonName());
        }
        resp.setEventType(tournament.getTournamentType());
        resp.setEventTypeName(resolveEventTypeName(tournament.getTournamentType()));
        resp.setTournamentId(toStringId(season.getTournamentId()));
        resp.setTournamentNo(tournament.getTournamentNo());
        resp.setTournamentName(tournament.getTournamentName());
        resp.setGameTerminal(tournament.getGameTerminal());
        if (StringUtils.hasText(tournament.getGameTerminal())) {
            String platformCode = GameTerminalSupport.normalize(tournament.getGameTerminal());
            resp.setPlatformCode(platformCode);
            resp.setPlatformName(GameTerminalSupport.formatDisplayName(tournament.getGameTerminal()));
        }
        resp.setTournamentStatus(tournament.getTournamentStatus());
        resp.setSeasonStatus(season.getSeasonStatus());
        resp.setRegistrationStatus(registrationStatus);
        if (team != null) {
            resp.setTeamId(toStringId(team.getId()));
            resp.setTeamName(team.getTeamName());
            resp.setTeamCertificationStatus(team.getAuditStatus());
        }
        resp.setMinRosterCount(season.getMinRosterCount());
        resp.setMaxRosterCount(season.getMaxRosterCount());
        resp.setRegistrationStartTime(resolveSignupStartTime(season, tournament));
        resp.setRegistrationEndTime(resolveSignupEndTime(season, tournament));
        resp.setSignupStartTime(resp.getRegistrationStartTime());
        resp.setSignupEndTime(resp.getRegistrationEndTime());
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
                .eq(BasketTournamentRegistration::getRegistrationStatus, TournamentRegistrationStatusEnum.APPROVED.getCode())
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

    private void assertRegistrationEditable(BasketTournamentRegistration registration) {
        if (isRegistrationRosterLocked(registration)) {
            throw new ServiceException(ROSTER_LOCKED_MESSAGE);
        }
        if (!TournamentRegistrationStatusEnum.fromCode(registration.getRegistrationStatus()).canEdit()) {
            throw new ServiceException("当前状态不可编辑");
        }
    }

    private boolean isRegistrationRosterLocked(BasketTournamentRegistration registration) {
        if (registration == null) {
            return false;
        }
        if (Objects.equals(TournamentRegistrationStatusEnum.APPROVED.getCode(), registration.getRegistrationStatus())) {
            return true;
        }
        BasketTeamSeasonProfile eventTeam = teamSeasonProfileDao.selectOne(new LambdaQueryWrapper<BasketTeamSeasonProfile>()
                .eq(BasketTeamSeasonProfile::getRegistrationId, registration.getId())
                .eq(BasketTeamSeasonProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        return eventTeam != null && Integer.valueOf(RosterLockStatusEnum.LOCKED.getCode()).equals(eventTeam.getRosterLockStatus());
    }

    private void applyRegistrationPermissionFields(TeamRegistrationDetailResp resp,
                                                   BasketTournamentRegistration registration,
                                                   TournamentRegistrationStatusEnum status) {
        boolean rosterLocked = isRegistrationRosterLocked(registration);
        resp.setRosterLocked(rosterLocked);
        resp.setRosterLockStatusText(rosterLocked ? "已锁定" : "未锁定");
        resp.setRosterLockedHint(rosterLocked ? ROSTER_LOCKED_HINT : null);
        resp.setCanRequestRosterChange(false);
        resp.setCanEdit(!rosterLocked && status.canEdit());
        resp.setCanSubmit(!rosterLocked && status.canSubmit());
        resp.setCanWithdraw(status.canWithdraw());
        resp.setCanDeleteDraft(!rosterLocked && status.canDeleteDraft());
    }

    private void applyRegistrationPermissionFields(TeamRegistrationItemResp resp,
                                                   BasketTournamentRegistration registration,
                                                   TournamentRegistrationStatusEnum status) {
        boolean rosterLocked = isRegistrationRosterLocked(registration);
        resp.setRosterLocked(rosterLocked);
        resp.setRosterLockStatusText(rosterLocked ? "已锁定" : "未锁定");
        resp.setCanEdit(!rosterLocked && status.canEdit());
        resp.setCanSubmit(!rosterLocked && status.canSubmit());
    }

    private void validateRosterSize(List<TeamRegistrationRosterItemReq> items, BasketTournamentSeason season) {
        int size = items.size();
        int minRoster = resolveMinRosterCount(season);
        int maxRoster = resolveMaxRosterCount(season);
        if (size < minRoster) {
            throw new ServiceException("阵容人数不足，至少需要 " + minRoster + " 人");
        }
        if (size > maxRoster) {
            throw new ServiceException("阵容人数超出上限，最多 " + maxRoster + " 人");
        }
    }

    private int resolveMinRosterCount(BasketTournamentSeason season) {
        if (season == null || season.getMinRosterCount() == null) {
            return DEFAULT_MIN_ROSTER_COUNT;
        }
        return Math.max(DEFAULT_MIN_ROSTER_COUNT,
                Math.min(season.getMinRosterCount(), DEFAULT_MAX_ROSTER_COUNT));
    }

    private int resolveMaxRosterCount(BasketTournamentSeason season) {
        if (season == null || season.getMaxRosterCount() == null) {
            return DEFAULT_MAX_ROSTER_COUNT;
        }
        return Math.min(DEFAULT_MAX_ROSTER_COUNT,
                Math.max(season.getMaxRosterCount(), resolveMinRosterCount(season)));
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
        resp.setPlatformCode(registration.getPlatformCode());
        resp.setPlatformName(eventPlatformSupport.resolvePlatformName(registration.getPlatformCode()));
        resp.setContactName(registration.getContactName());
        resp.setContactMobileMasked(DataMaskUtil.maskMobile(registration.getContactMobile()));
        resp.setContactEmailMasked(DataMaskUtil.maskEmail(registration.getContactEmail()));
        resp.setRegistrationStatus(registration.getRegistrationStatus());
        resp.setRegistrationStatusText(status.getDisplayDescription());
        resp.setLineupCount(registration.getLineupCount());
        resp.setRemark(registration.getRemark());
        applyRegistrationPermissionFields(resp, registration, status);
        resp.setSubmittedTime(registration.getSubmittedTime());
        resp.setRegistrationEndTime(season == null ? null : season.getRegistrationEndTime());
        if (season != null) {
            resp.setMinRosterCount(resolveMinRosterCount(season));
            resp.setMaxRosterCount(resolveMaxRosterCount(season));
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
        resp.setPlatformCode(registration.getPlatformCode());
        resp.setPlatformName(eventPlatformSupport.resolvePlatformName(registration.getPlatformCode()));
        resp.setRegistrationStatus(registration.getRegistrationStatus());
        resp.setRegistrationStatusText(status.getDisplayDescription());
        resp.setLineupCount(registration.getLineupCount());
        applyRegistrationPermissionFields(resp, registration, status);
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
