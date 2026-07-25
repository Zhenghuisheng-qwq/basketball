package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketDisciplineRecordDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.BasketTournamentDao;
import com.zhs.dao.BasketTournamentRegistrationAuditDao;
import com.zhs.dao.BasketTournamentRegistrationDao;
import com.zhs.dao.BasketTournamentRegistrationPlayerDao;
import com.zhs.dao.BasketTournamentSeasonDao;
import com.zhs.enums.DisciplineStatusEnum;
import com.zhs.enums.RegistrationPlayerValidationStatusEnum;
import com.zhs.enums.TeamMemberRoleEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.TournamentRegistrationStatusEnum;
import com.zhs.enums.TournamentSeasonStatusEnum;
import com.zhs.enums.TournamentStatusEnum;
import com.zhs.exception.ServiceException;
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
import com.zhs.response.team.AvailableSeasonItemResp;
import com.zhs.response.team.TeamRegistrationAuditItemResp;
import com.zhs.response.team.TeamRegistrationDetailResp;
import com.zhs.response.team.TeamRegistrationItemResp;
import com.zhs.response.team.TeamRegistrationPlayerItemResp;
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
    private static final String BIZ_TYPE_TEAM_REGISTRATION = "TEAM_REGISTRATION";

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
    private BasketDisciplineRecordDao disciplineRecordDao;
    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private IBusinessMessageService businessMessageService;

    @Override
    public IPage<AvailableSeasonItemResp> pageAvailableSeasons(String teamId, Integer currentPage, Integer pageSize) {
        Long teamPk = parseLongId(teamId, "球队主键");
        Long memberId = UserContext.getRequiredUserId();
        captainPermissionService.requireTeamCaptain(memberId, teamPk);
        Date now = new Date();

        List<BasketTournamentSeason> seasons = seasonDao.selectList(new LambdaQueryWrapper<BasketTournamentSeason>()
                .eq(BasketTournamentSeason::getIsDelete, NOT_DELETED)
                .eq(BasketTournamentSeason::getSeasonStatus, TournamentSeasonStatusEnum.REGISTRATION_OPEN.getCode())
                .le(BasketTournamentSeason::getRegistrationStartTime, now)
                .ge(BasketTournamentSeason::getRegistrationEndTime, now)
                .orderByDesc(BasketTournamentSeason::getRegistrationStartTime));

        Map<Long, BasketTournament> tournamentMap = loadTournamentMap(seasons);
        Map<Long, BasketTournamentRegistration> existingMap = loadExistingRegistrationMap(teamPk, seasons);

        List<AvailableSeasonItemResp> items = new ArrayList<>();
        for (BasketTournamentSeason season : seasons) {
            BasketTournament tournament = tournamentMap.get(season.getTournamentId());
            if (tournament == null || !Objects.equals(TournamentStatusEnum.ACTIVE.getCode(), tournament.getTournamentStatus())) {
                continue;
            }
            AvailableSeasonItemResp resp = new AvailableSeasonItemResp();
            resp.setSeasonId(toStringId(season.getId()));
            resp.setSeasonNo(season.getSeasonNo());
            resp.setSeasonName(season.getSeasonName());
            resp.setTournamentId(toStringId(season.getTournamentId()));
            resp.setTournamentName(tournament.getTournamentName());
            resp.setSeasonStatus(season.getSeasonStatus());
            resp.setMinRosterCount(season.getMinRosterCount());
            resp.setMaxRosterCount(season.getMaxRosterCount());
            resp.setRegistrationStartTime(season.getRegistrationStartTime());
            resp.setRegistrationEndTime(season.getRegistrationEndTime());
            BasketTournamentRegistration existing = existingMap.get(season.getId());
            resp.setHasRegistration(existing != null);
            resp.setRegistrationId(existing == null ? null : toStringId(existing.getId()));
            items.add(resp);
        }
        return toPage(items, currentPage, pageSize);
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

        assertNoExistingRegistration(teamPk, seasonPk);

        BasketTournamentRegistration registration = new BasketTournamentRegistration();
        registration.setRegistrationNo("RG" + IdUtil.getSnowflakeNextIdStr());
        registration.setSeasonId(seasonPk);
        registration.setTeamId(teamPk);
        registration.setCaptainMemberId(memberId);
        registration.setCaptainPlayerId(team.getCaptainPlayerId());
        registration.setContactName(trimToNull(req.getContactName()));
        registration.setContactMobile(trimToNull(req.getContactMobile()));
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

        registration.setContactName(trimToNull(req.getContactName()));
        registration.setContactMobile(trimToNull(req.getContactMobile()));
        registration.setContactEmail(trimToNull(req.getContactEmail()));
        registration.setRemark(trimToNull(req.getRemark()));
        registrationDao.updateById(registration);

        BasketTeam team = teamDao.selectById(registration.getTeamId());
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
                .set(BasketTournamentRegistration::getRegistrationStatus, TournamentRegistrationStatusEnum.SUBMITTED.getCode())
                .set(BasketTournamentRegistration::getSubmittedTime, now));
        if (updated == 0) {
            throw new ServiceException("提交失败，报名状态已变更");
        }
        registration = registrationDao.selectById(registrationPk);

        businessMessageService.sendSiteMessage(memberId, "赛事报名已提交",
                "您的赛事报名已提交，请等待审核", BIZ_TYPE_TEAM_REGISTRATION, registration.getId(),
                "TEAM_REGISTRATION", String.valueOf(registration.getId()));

        BasketTeam team = teamDao.selectById(registration.getTeamId());
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
                .eq(BasketTournamentRegistration::getRegistrationStatus, TournamentRegistrationStatusEnum.SUBMITTED.getCode())
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
        Date now = new Date();
        if (!Objects.equals(TournamentSeasonStatusEnum.REGISTRATION_OPEN.getCode(), season.getSeasonStatus())) {
            throw new ServiceException("赛季未开放报名");
        }
        if (season.getRegistrationStartTime() != null && season.getRegistrationStartTime().after(now)) {
            throw new ServiceException("报名尚未开始");
        }
        if (season.getRegistrationEndTime() != null && season.getRegistrationEndTime().before(now)) {
            throw new ServiceException("报名已截止");
        }
        return season;
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
        resp.setRegistrationStatusText(status.getDescription());
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
            item.setPlayerId(toStringId(player.getPlayerId()));
            item.setJerseyNo(player.getJerseyNo());
            item.setIsStarting(player.getIsStarting());
            item.setIsCaptain(player.getIsCaptain());
            item.setValidationStatus(player.getValidationStatus());
            item.setSortNo(player.getSortNo());
            BasketPlayerProfile profile = playerMap.get(player.getPlayerId());
            if (profile != null) {
                item.setGameNickname(profile.getGameNickname());
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
        resp.setRegistrationStatusText(status.getDescription());
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
