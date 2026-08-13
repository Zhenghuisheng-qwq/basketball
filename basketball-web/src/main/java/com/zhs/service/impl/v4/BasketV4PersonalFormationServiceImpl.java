package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.v4.*;
import com.zhs.enums.TeamJoinSourceEnum;
import com.zhs.enums.v4.*;
import com.zhs.exception.ServiceException;
import com.zhs.mapper.v4.BasketV4PersonalFormationMapper;
import com.zhs.request.team.TeamCreateReq;
import com.zhs.response.team.TeamDetailResp;
import com.zhs.service.IAccountTeamService;
import com.zhs.service.team.TeamMemberJoinHelper;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.*;
import com.zhs.request.v4.V4PersonalFormationBindTeamReq;
import com.zhs.response.v4.V4PersonalFormationAvailableTeamResp;
import com.zhs.response.v4.V4PersonalFormationDetailResp;
import com.zhs.response.v4.V4PersonalFormationTaskResp;
import com.zhs.service.v4.IBasketV4PersonalFormationService;
import com.zhs.support.GameTerminalSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketV4PersonalFormationServiceImpl implements IBasketV4PersonalFormationService {

    private static final Integer NOT_DELETED = 0;
    private static final int TEAM_STATUS_ACTIVE = 1;
    private static final int TEAM_AUDIT_APPROVED = 1;
    private static final int MEMBER_ACTIVE = 1;
    private static final int IS_CURRENT = 1;
    private static final int PLAYER_CERT_APPROVED = 3;
    private static final int PLATFORM_ACCOUNT_ACTIVE = 1;
    private static final String EVENT_TEAM_STATUS_ACTIVE = "ACTIVE";
    private static final String EVENT_PLAYER_STATUS_ACTIVE = "ACTIVE";

    @Resource
    private BasketV4PersonalFormationMapper personalFormationMapper;
    @Resource
    private BasketPersonalDrawTeamDao drawTeamDao;
    @Resource
    private BasketPersonalDrawBatchDao drawBatchDao;
    @Resource
    private BasketPersonalDrawAssignmentDao drawAssignmentDao;
    @Resource
    private BasketPersonalRegistrationDao personalRegistrationDao;
    @Resource
    private BasketPersonalRegistrationAuditDao personalRegistrationAuditDao;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketRegistrationDao registrationDao;
    @Resource
    private BasketRegistrationPlayerDao registrationPlayerDao;
    @Resource
    private BasketRegistrationAuditDao registrationAuditDao;
    @Resource
    private BasketEventTeamDao eventTeamDao;
    @Resource
    private BasketEventPlayerDao eventPlayerDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Resource
    private BasketDivisionFormatSnapshotDao formatSnapshotDao;
    @Resource
    private IAccountTeamService accountTeamService;
    @Resource
    private TeamMemberJoinHelper teamMemberJoinHelper;

    @Override
    public TeamDetailResp createLongTermTeam(Long drawTeamId, TeamCreateReq req, String clientIp) {
        BasketPersonalDrawTeam team = requireCaptainTeam(drawTeamId);
        rejectIfVersioned(team.getDivisionId());
        return accountTeamService.createForPersonalFormation(drawTeamId, req, clientIp);
    }

    @Override
    public List<V4PersonalFormationTaskResp> listFormationTasks() {
        List<V4PersonalFormationTaskResp> tasks =
                personalFormationMapper.selectFormationTasksByCaptainUserId(UserContext.getRequiredMemberId());
        for (V4PersonalFormationTaskResp task : tasks) {
            if (task.getDrawTeamId() != null) {
                task.setMembers(personalFormationMapper.selectFormationMembers(task.getDrawTeamId()));
            }
            task.setNeedsLongTermBind(!isVersionedWorkflow(task.getRosterWorkflowMode()));
        }
        return tasks;
    }

    @Override
    public V4PersonalFormationTaskResp getFormationDetail(Long drawTeamId) {
        BasketPersonalDrawTeam drawTeam = requireCaptainTeam(drawTeamId);
        V4PersonalFormationDetailResp detail = personalFormationMapper.selectFormationDetail(drawTeamId);
        if (detail == null) {
            throw new ServiceException("组队任务不存在");
        }
        V4PersonalFormationTaskResp resp = new V4PersonalFormationTaskResp();
        resp.setDrawTeamId(detail.getDrawTeamId());
        resp.setDrawBatchId(detail.getDrawBatchId());
        resp.setDivisionId(detail.getDivisionId());
        resp.setDivisionName(detail.getDivisionName());
        resp.setTemporaryTeamCode(detail.getTemporaryTeamCode());
        resp.setTemporaryTeamName(detail.getTemporaryTeamName());
        resp.setExpectedMemberCount(detail.getExpectedMemberCount());
        resp.setActualMemberCount(detail.getActualMemberCount());
        resp.setLongTermTeamId(detail.getLongTermTeamId());
        resp.setLongTermTeamName(detail.getLongTermTeamName());
        resp.setFormationStatus(detail.getFormationStatus());
        resp.setConvertedRegistrationId(detail.getConvertedRegistrationId());
        resp.setMembers(personalFormationMapper.selectFormationMembers(drawTeamId));
        resp.setNeedsLongTermBind(!isVersioned(drawTeam.getDivisionId()));
        return resp;
    }

    @Override
    public List<V4PersonalFormationAvailableTeamResp> listAvailableTeams(Long drawTeamId) {
        BasketPersonalDrawTeam drawTeam = requireCaptainTeam(drawTeamId);
        rejectIfVersioned(drawTeam.getDivisionId());
        BasketDivision division = requireDivision(drawTeam.getDivisionId());
        List<BasketPersonalDrawAssignment> assignments = listAssignments(drawTeam.getId());
        Set<Long> assignedPlayerIds = assignments.stream()
                .map(BasketPersonalDrawAssignment::getPlayerId)
                .collect(java.util.stream.Collectors.toSet());
        Long memberId = UserContext.getRequiredMemberId();
        List<V4PersonalFormationAvailableTeamResp> candidates =
                personalFormationMapper.selectCandidateTeamsByCaptainMemberId(memberId);
        List<V4PersonalFormationAvailableTeamResp> result = new ArrayList<>();
        for (V4PersonalFormationAvailableTeamResp candidate : candidates) {
            evaluateTeamCandidate(candidate, drawTeam, division, assignments, assignedPlayerIds);
            result.add(candidate);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindLongTermTeam(V4PersonalFormationBindTeamReq req) {
        if (req == null || req.getDrawTeamId() == null || req.getLongTermTeamId() == null) {
            throw new ServiceException("drawTeamId / longTermTeamId 不能为空");
        }
        BasketPersonalDrawTeam team = requireCaptainTeam(req.getDrawTeamId());
        rejectIfVersioned(team.getDivisionId());
        if (team.getConvertedRegistrationId() != null) {
            throw new ServiceException("该临时队伍已转换");
        }
        if (team.getLongTermTeamId() != null && Objects.equals(team.getLongTermTeamId(), req.getLongTermTeamId())) {
            return;
        }
        if (team.getLongTermTeamId() != null) {
            throw new ServiceException("该临时队伍已绑定其他长期球队");
        }
        requireConfirmedBatch(team.getDrawBatchId());
        BasketDivision division = requireDivision(team.getDivisionId());
        List<BasketPersonalDrawAssignment> assignments = listAssignments(team.getId());
        validateTeamForBind(req.getLongTermTeamId(), team, division);

        BasketTeam longTermTeam = teamDao.selectById(req.getLongTermTeamId());
        if (longTermTeam == null) {
            throw new ServiceException("球队不存在");
        }
        autoJoinAssignedMembers(longTermTeam, team, assignments);

        Date now = new Date();
        for (BasketPersonalDrawAssignment assignment : assignments) {
            try {
                requireTeamMember(req.getLongTermTeamId(), assignment.getPlayerId());
                assignment.setJoinStatus(JoinStatusEnum.JOINED.getCode());
            } catch (ServiceException ex) {
                assignment.setJoinStatus(JoinStatusEnum.WAITING_INVITE.getCode());
            }
            assignment.setUpdateTime(now);
            drawAssignmentDao.updateById(assignment);
        }

        List<BasketPersonalDrawAssignment> refreshed = listAssignments(team.getId());
        boolean allJoined = refreshed.stream()
                .allMatch(a -> JoinStatusEnum.JOINED.getCode().equals(a.getJoinStatus()));
        team.setLongTermTeamId(req.getLongTermTeamId());
        if (allJoined && !refreshed.isEmpty()) {
            team.setFormationStatus(FormationStatusEnum.READY_TO_CONFIRM.getCode());
        } else {
            team.setFormationStatus(FormationStatusEnum.TEAM_FORMING.getCode());
        }
        team.setUpdateTime(now);
        drawTeamDao.updateById(team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmFormation(Long drawTeamId) {
        BasketPersonalDrawTeam team = requireCaptainTeam(drawTeamId);
        rejectIfVersioned(team.getDivisionId());
        if (team.getConvertedRegistrationId() != null) {
            return team.getConvertedRegistrationId();
        }
        if (team.getLongTermTeamId() == null) {
            throw new ServiceException("请先绑定长期球队");
        }
        BasketDivision division = requireDivision(team.getDivisionId());
        BasketPersonalDrawBatch batch = requireConfirmedBatch(team.getDrawBatchId());
        List<BasketPersonalDrawAssignment> assignments = listAssignments(team.getId());
        validateFormation(team, division, assignments);

        Date now = new Date();
        Long memberId = UserContext.getRequiredMemberId();
        Long captainPlayerId = team.getCaptainPlayerId();
        List<Long> playerIds = new ArrayList<>();
        for (BasketPersonalDrawAssignment assignment : assignments) {
            playerIds.add(assignment.getPlayerId());
            if (AssignmentRoleEnum.CAPTAIN.getCode().equals(assignment.getAssignmentRole())) {
                captainPlayerId = assignment.getPlayerId();
            }
        }
        BasketPlayerProfile captainProfile = requireCertifiedPlayer(captainPlayerId);

        BasketRegistration registration = new BasketRegistration();
        registration.setSeasonId(division.getSeasonId());
        registration.setDivisionId(division.getId());
        registration.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        registration.setRegistrationSource(RegistrationSourceEnum.PERSONAL_FORMATION.getCode());
        registration.setSourcePersonalDrawTeamId(team.getId());
        registration.setSourcePersonalDrawBatchId(batch.getId());
        registration.setTeamId(team.getLongTermTeamId());
        registration.setCaptainPlayerId(captainPlayerId);
        registration.setCaptainMemberId(captainProfile.getMemberId());
        registration.setRegistrationStatus(RegistrationStatusEnum.APPROVED.getCode());
        registration.setLineupCount(playerIds.size());
        registration.setSubmitTime(now);
        registration.setApproveTime(now);
        registration.setCreateTime(now);
        registration.setUpdateTime(now);
        registration.setIsDelete(NOT_DELETED);
        registrationDao.insert(registration);

        List<BasketRegistrationPlayer> players = buildLineup(division, registration.getId(), team.getLongTermTeamId(), captainPlayerId, playerIds);
        for (BasketRegistrationPlayer player : players) {
            registrationPlayerDao.insert(player);
        }

        BasketRegistrationAudit audit = new BasketRegistrationAudit();
        audit.setRegistrationId(registration.getId());
        audit.setDivisionId(division.getId());
        audit.setSeasonId(division.getSeasonId());
        audit.setAuditAction("APPROVE");
        audit.setAuditResult("APPROVED");
        audit.setBeforeStatus(RegistrationStatusEnum.PENDING_REVIEW.getCode());
        audit.setAfterStatus(RegistrationStatusEnum.APPROVED.getCode());
        audit.setAuditUserId(memberId);
        audit.setAuditTime(now);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        registrationAuditDao.insert(audit);

        generateEventSnapshot(registration, division, players);

        team.setConvertedRegistrationId(registration.getId());
        team.setFormationStatus(FormationStatusEnum.CONVERTED.getCode());
        team.setConfirmedBy(memberId);
        team.setConfirmedTime(now);
        team.setUpdateTime(now);
        drawTeamDao.updateById(team);

        for (BasketPersonalDrawAssignment assignment : assignments) {
            assignment.setJoinStatus(JoinStatusEnum.CONFIRMED.getCode());
            assignment.setUpdateTime(now);
            drawAssignmentDao.updateById(assignment);
            BasketPersonalRegistration personalReg = personalRegistrationDao.selectById(assignment.getPersonalRegistrationId());
            if (personalReg != null) {
                String before = personalReg.getRegistrationStatus();
                personalReg.setConvertedRegistrationId(registration.getId());
                personalReg.setRegistrationStatus(PersonalRegistrationStatusEnum.CONVERTED.getCode());
                personalReg.setUpdateTime(now);
                personalRegistrationDao.updateById(personalReg);
                writePersonalAudit(personalReg, before, PersonalRegistrationStatusEnum.CONVERTED.getCode(),
                        PersonalAuditActionEnum.CONVERT, null);
            }
        }
        return registration.getId();
    }

    private void generateEventSnapshot(BasketRegistration registration, BasketDivision division,
                                         List<BasketRegistrationPlayer> players) {
        BasketTeam team = teamDao.selectById(registration.getTeamId());
        if (team == null) {
            throw new ServiceException("来源球队不存在");
        }
        Date now = new Date();
        BasketEventTeam eventTeam = new BasketEventTeam();
        eventTeam.setSeasonId(registration.getSeasonId());
        eventTeam.setDivisionId(registration.getDivisionId());
        eventTeam.setRegistrationId(registration.getId());
        eventTeam.setSourceTeamId(registration.getTeamId());
        eventTeam.setTeamNameSnapshot(team.getTeamName());
        eventTeam.setTeamLogoFileId(team.getLogoFileId());
        eventTeam.setTeamShortName(team.getShortName());
        eventTeam.setCaptainPlayerId(registration.getCaptainPlayerId());
        eventTeam.setCaptainMemberId(registration.getCaptainMemberId());
        eventTeam.setEventTeamNo(team.getTeamNo());
        eventTeam.setEventTeamStatus(EVENT_TEAM_STATUS_ACTIVE);
        eventTeam.setPlatformCode(division.getPlatformCode());
        eventTeam.setCreateTime(now);
        eventTeam.setUpdateTime(now);
        eventTeam.setIsDelete(NOT_DELETED);
        eventTeamDao.insert(eventTeam);

        for (BasketRegistrationPlayer regPlayer : players) {
            BasketEventPlayer eventPlayer = new BasketEventPlayer();
            eventPlayer.setSeasonId(registration.getSeasonId());
            eventPlayer.setDivisionId(registration.getDivisionId());
            eventPlayer.setEventTeamId(eventTeam.getId());
            eventPlayer.setSourcePlayerId(regPlayer.getPlayerId());
            eventPlayer.setMemberId(regPlayer.getMemberId());
            eventPlayer.setPlatformIdentityId(regPlayer.getPlatformIdentityId());
            eventPlayer.setPlatformCode(regPlayer.getPlatformCode());
            eventPlayer.setGameIdSnapshot(regPlayer.getGameIdSnapshot());
            eventPlayer.setPlayerNameSnapshot(regPlayer.getPlayerNameSnapshot());
            eventPlayer.setPosition(regPlayer.getPosition());
            eventPlayer.setJerseyNo(regPlayer.getJerseyNo());
            eventPlayer.setIsCaptain(regPlayer.getIsCaptain());
            eventPlayer.setIsStarter(regPlayer.getIsStarter());
            eventPlayer.setEventPlayerStatus(EVENT_PLAYER_STATUS_ACTIVE);
            eventPlayer.setCreateTime(now);
            eventPlayer.setUpdateTime(now);
            eventPlayer.setIsDelete(NOT_DELETED);
            eventPlayerDao.insert(eventPlayer);
        }
    }

    private List<BasketRegistrationPlayer> buildLineup(BasketDivision division, Long registrationId,
                                                         Long teamId, Long captainPlayerId, List<Long> playerIds) {
        List<BasketRegistrationPlayer> rows = new ArrayList<>();
        Date now = new Date();
        int sort = 0;
        for (Long playerId : playerIds) {
            requireTeamMember(teamId, playerId);
            BasketPlayerProfile profile = requireCertifiedPlayer(playerId);
            BasketPlayerPlatformAccount account = requirePlatformIdentity(playerId, division.getPlatformCode());
            BasketRegistrationPlayer row = new BasketRegistrationPlayer();
            row.setRegistrationId(registrationId);
            row.setDivisionId(division.getId());
            row.setSeasonId(division.getSeasonId());
            row.setTeamId(teamId);
            row.setPlayerId(playerId);
            row.setMemberId(profile.getMemberId());
            row.setPlatformIdentityId(account.getId());
            row.setPlatformCode(GameTerminalSupport.normalize(account.getPlatform()));
            row.setGameIdSnapshot(account.getDisplayGameId());
            row.setPlayerNameSnapshot(profile.getGameNickname());
            row.setPosition(profile.getPositionCode());
            row.setIsCaptain(playerId.equals(captainPlayerId) ? 1 : 0);
            row.setIsStarter(1);
            row.setSortOrder(sort++);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            rows.add(row);
        }
        return rows;
    }

    private void validateFormation(BasketPersonalDrawTeam team, BasketDivision division,
                                   List<BasketPersonalDrawAssignment> assignments) {
        if (assignments.isEmpty()) {
            throw new ServiceException("临时队伍无成员");
        }
        long captainCount = assignments.stream()
                .filter(a -> AssignmentRoleEnum.CAPTAIN.getCode().equals(a.getAssignmentRole()))
                .count();
        if (captainCount != 1) {
            throw new ServiceException("临时队伍必须有且仅有一名队长");
        }
        Set<Long> assignedPlayerIds = assignments.stream()
                .map(BasketPersonalDrawAssignment::getPlayerId)
                .collect(Collectors.toSet());
        for (BasketPersonalDrawAssignment assignment : assignments) {
            if (!JoinStatusEnum.JOINED.getCode().equals(assignment.getJoinStatus())
                    && !JoinStatusEnum.CONFIRMED.getCode().equals(assignment.getJoinStatus())) {
                throw new ServiceException("球员 " + playerDisplayName(assignment.getPlayerId()) + " 尚未加入长期球队");
            }
            try {
                requireTeamMember(team.getLongTermTeamId(), assignment.getPlayerId());
            } catch (ServiceException ex) {
                throw new ServiceException("球员 " + playerDisplayName(assignment.getPlayerId()) + " 尚未加入长期球队");
            }
            requirePlatformIdentity(assignment.getPlayerId(), division.getPlatformCode());
            Long conflictCount = eventPlayerDao.selectCount(new LambdaQueryWrapper<BasketEventPlayer>()
                    .eq(BasketEventPlayer::getDivisionId, division.getId())
                    .eq(BasketEventPlayer::getSourcePlayerId, assignment.getPlayerId())
                    .eq(BasketEventPlayer::getEventPlayerStatus, EVENT_PLAYER_STATUS_ACTIVE)
                    .eq(BasketEventPlayer::getIsDelete, NOT_DELETED));
            if (conflictCount != null && conflictCount > 0) {
                throw new ServiceException("球员 " + playerDisplayName(assignment.getPlayerId()) + " 已在本赛区其他正式队伍中");
            }
        }
        List<BasketTeamMember> teamMembers = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, team.getLongTermTeamId())
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, MEMBER_ACTIVE)
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        for (BasketTeamMember member : teamMembers) {
            if (!assignedPlayerIds.contains(member.getPlayerId())) {
                throw new ServiceException("球员 " + playerDisplayName(member.getPlayerId()) + " 未被分配到当前抽签队伍");
            }
        }
        GameModeEnum gameMode = GameModeEnum.resolveOrLegacyDefault(division.getGameMode());
        int min = division.getLineupMinCount() != null ? division.getLineupMinCount() : gameMode.getMinRosterCount();
        int max = division.getLineupMaxCount() != null ? division.getLineupMaxCount() : gameMode.getMaxRosterCount();
        int lineupSize = assignments.size();
        if (lineupSize < min || lineupSize > max) {
            throw new ServiceException("当前赛区为 " + gameMode.getDescription()
                    + "，阵容人数应为 " + min + "～" + max + " 人");
        }
    }

    private void autoJoinAssignedMembers(BasketTeam longTermTeam, BasketPersonalDrawTeam drawTeam,
                                         List<BasketPersonalDrawAssignment> assignments) {
        for (BasketPersonalDrawAssignment assignment : assignments) {
            if (drawTeam.getCaptainPlayerId() != null
                    && drawTeam.getCaptainPlayerId().equals(assignment.getPlayerId())) {
                continue;
            }
            try {
                requireTeamMember(longTermTeam.getId(), assignment.getPlayerId());
            } catch (ServiceException ex) {
                BasketPlayerProfile profile = requireCertifiedPlayer(assignment.getPlayerId());
                teamMemberJoinHelper.addMember(longTermTeam, profile.getMemberId(),
                        assignment.getPlayerId(), TeamJoinSourceEnum.PERSONAL_DRAW);
            }
        }
    }

    private String playerDisplayName(Long playerId) {
        if (playerId == null) {
            return "未知球员";
        }
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile != null && profile.getGameNickname() != null && !profile.getGameNickname().isBlank()) {
            return profile.getGameNickname();
        }
        return String.valueOf(playerId);
    }

    private BasketPersonalDrawTeam requireCaptainTeam(Long drawTeamId) {
        BasketPersonalDrawTeam team = drawTeamDao.selectById(drawTeamId);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("临时队伍不存在");
        }
        if (!UserContext.getRequiredMemberId().equals(team.getCaptainUserId())) {
            throw new ServiceException("无权操作该组队任务");
        }
        return team;
    }

    private BasketPersonalDrawBatch requireConfirmedBatch(Long batchId) {
        BasketPersonalDrawBatch batch = drawBatchDao.selectById(batchId);
        if (batch == null || !DrawBatchStatusEnum.CONFIRMED.getCode().equals(batch.getBatchStatus())) {
            throw new ServiceException("抽签批次尚未确认");
        }
        return batch;
    }

    private List<BasketPersonalDrawAssignment> listAssignments(Long drawTeamId) {
        return drawAssignmentDao.selectList(new LambdaQueryWrapper<BasketPersonalDrawAssignment>()
                .eq(BasketPersonalDrawAssignment::getDrawTeamId, drawTeamId)
                .eq(BasketPersonalDrawAssignment::getIsDelete, NOT_DELETED)
                .orderByAsc(BasketPersonalDrawAssignment::getSortOrder));
    }

    private BasketDivision requireDivision(Long divisionId) {
        BasketDivision division = divisionDao.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }

    private void rejectIfVersioned(Long divisionId) {
        if (isVersioned(divisionId)) {
            throw new ServiceException("VERSIONED PERSONAL Division 临时队长无需绑定长期球队，锁定后请使用本队比赛提交");
        }
    }

    private boolean isVersioned(Long divisionId) {
        BasketDivisionFormatSnapshot snapshot = formatSnapshotDao.selectOne(new LambdaQueryWrapper<BasketDivisionFormatSnapshot>()
                .eq(BasketDivisionFormatSnapshot::getDivisionId, divisionId)
                .eq(BasketDivisionFormatSnapshot::getIsCurrent, 1)
                .eq(BasketDivisionFormatSnapshot::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        return snapshot != null && isVersionedWorkflow(snapshot.getRosterWorkflowMode());
    }

    private boolean isVersionedWorkflow(String rosterWorkflowMode) {
        return "VERSIONED".equals(rosterWorkflowMode);
    }

    private void requireValidTeam(Long teamId) {
        BasketTeam team = teamDao.selectById(teamId);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        if (!Integer.valueOf(TEAM_STATUS_ACTIVE).equals(team.getTeamStatus())) {
            throw new ServiceException("球队状态无效");
        }
        if (!Integer.valueOf(TEAM_AUDIT_APPROVED).equals(team.getAuditStatus())) {
            throw new ServiceException("球队尚未审核通过");
        }
    }

    private void requireTeamMember(Long teamId, Long playerId) {
        Long count = teamMemberDao.selectCount(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getPlayerId, playerId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, MEMBER_ACTIVE)
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
        if (count == null || count <= 0) {
            throw new ServiceException("球员不是该球队当前有效成员");
        }
    }

    private BasketPlayerProfile requireCertifiedPlayer(Long playerId) {
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile == null || !NOT_DELETED.equals(profile.getIsDelete())) {
            throw new ServiceException("球员不存在");
        }
        if (!Integer.valueOf(PLAYER_CERT_APPROVED).equals(profile.getCertificationStatus())) {
            throw new ServiceException("球员尚未完成认证");
        }
        return profile;
    }

    private BasketPlayerPlatformAccount requirePlatformIdentity(Long playerId, String divisionPlatformCode) {
        List<BasketPlayerPlatformAccount> accounts = platformAccountDao.selectList(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .eq(BasketPlayerPlatformAccount::getPlayerId, playerId)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED));
        for (BasketPlayerPlatformAccount account : accounts) {
            if (Integer.valueOf(PLATFORM_ACCOUNT_ACTIVE).equals(account.getAccountStatus())
                    && GameTerminalSupport.matchesDivisionPlatform(divisionPlatformCode, account.getPlatform())) {
                return account;
            }
        }
        throw new ServiceException("球员缺少对应平台身份");
    }

    private void evaluateTeamCandidate(V4PersonalFormationAvailableTeamResp candidate,
                                       BasketPersonalDrawTeam drawTeam,
                                       BasketDivision division,
                                       List<BasketPersonalDrawAssignment> assignments,
                                       Set<Long> assignedPlayerIds) {
        candidate.setCanBind(false);
        candidate.setMatchedAssignedMemberCount(0);
        candidate.setMissingAssignedMemberCount(0);
        candidate.setExtraMemberCount(0);
        candidate.setCaptainMatched(false);
        try {
            validateTeamForBind(candidate.getTeamId(), drawTeam, division);
            int matched = 0;
            for (BasketPersonalDrawAssignment assignment : assignments) {
                try {
                    requireTeamMember(candidate.getTeamId(), assignment.getPlayerId());
                    matched++;
                } catch (ServiceException ignored) {
                    // count missing below
                }
            }
            candidate.setMatchedAssignedMemberCount(matched);
            candidate.setMissingAssignedMemberCount(assignments.size() - matched);
            List<BasketTeamMember> members = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                    .eq(BasketTeamMember::getTeamId, candidate.getTeamId())
                    .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                    .eq(BasketTeamMember::getMembershipStatus, MEMBER_ACTIVE)
                    .eq(BasketTeamMember::getIsDelete, NOT_DELETED));
            Set<Long> memberPlayerIds = members.stream().map(BasketTeamMember::getPlayerId).collect(java.util.stream.Collectors.toSet());
            int extra = 0;
            for (Long pid : memberPlayerIds) {
                if (!assignedPlayerIds.contains(pid)) {
                    extra++;
                }
            }
            candidate.setExtraMemberCount(extra);
            candidate.setCurrentMemberCount(members.size());
            if (drawTeam.getCaptainPlayerId() != null) {
                candidate.setCaptainMatched(memberPlayerIds.contains(drawTeam.getCaptainPlayerId()));
            }
            candidate.setCanBind(true);
            candidate.setUnavailableReason(null);
        } catch (ServiceException ex) {
            candidate.setUnavailableReason(ex.getMessage());
        }
    }

    private void validateTeamForBind(Long teamId, BasketPersonalDrawTeam drawTeam, BasketDivision division) {
        requireValidTeam(teamId);
        if (drawTeam.getCaptainPlayerId() != null) {
            requireTeamMember(teamId, drawTeam.getCaptainPlayerId());
        }
        Long occupied = drawTeamDao.selectCount(new LambdaQueryWrapper<BasketPersonalDrawTeam>()
                .eq(BasketPersonalDrawTeam::getLongTermTeamId, teamId)
                .ne(BasketPersonalDrawTeam::getId, drawTeam.getId())
                .notIn(BasketPersonalDrawTeam::getFormationStatus,
                        FormationStatusEnum.CONVERTED.getCode(), FormationStatusEnum.CANCELLED.getCode())
                .eq(BasketPersonalDrawTeam::getIsDelete, NOT_DELETED));
        if (occupied != null && occupied > 0) {
            throw new ServiceException("该球队已被其他成队任务占用");
        }
        Long eventTeamCount = eventTeamDao.selectCount(new LambdaQueryWrapper<BasketEventTeam>()
                .eq(BasketEventTeam::getDivisionId, division.getId())
                .eq(BasketEventTeam::getSourceTeamId, teamId)
                .eq(BasketEventTeam::getIsDelete, NOT_DELETED));
        if (eventTeamCount != null && eventTeamCount > 0) {
            throw new ServiceException("该球队已在本赛区生成正式参赛队");
        }
    }

    private void writePersonalAudit(BasketPersonalRegistration registration, String beforeStatus,
                                    String afterStatus, PersonalAuditActionEnum action, String remark) {
        Date now = new Date();
        BasketPersonalRegistrationAudit audit = new BasketPersonalRegistrationAudit();
        audit.setPersonalRegistrationId(registration.getId());
        audit.setDivisionId(registration.getDivisionId());
        audit.setBeforeStatus(beforeStatus);
        audit.setAfterStatus(afterStatus);
        audit.setAuditAction(action.getCode());
        audit.setAuditRemark(remark);
        audit.setOperatorId(UserContext.getRequiredMemberId());
        audit.setOperateTime(now);
        audit.setCreateTime(now);
        audit.setIsDelete(NOT_DELETED);
        personalRegistrationAuditDao.insert(audit);
    }
}
