package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.v4.*;
import com.zhs.enums.v4.GameModeEnum;
import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.enums.v4.RegistrationModeEnum;
import com.zhs.enums.v4.RegistrationStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.mapper.v4.BasketV4PlayerRegistrationMapper;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.*;
import com.zhs.request.v4.V4RegistrationIdReq;
import com.zhs.request.v4.V4RegistrationSaveReq;
import com.zhs.response.v4.V4AvailableDivisionResp;
import com.zhs.response.v4.V4MyRegistrationResp;
import com.zhs.response.v4.V4RegistrationDetailResp;
import com.zhs.response.v4.V4RegistrationPlayerItemResp;
import com.zhs.response.v4.V4RegistrationDivisionContextResp;
import com.zhs.response.v4.V4RegistrationRosterPlayerResp;
import com.zhs.support.v4.TeamRegistrationEligibility;
import com.zhs.support.v4.TeamRegistrationEligibilitySupport;
import com.zhs.service.v4.IBasketV4PlayerRegistrationService;
import com.zhs.support.CaptainGrantSupport;
import com.zhs.support.GameTerminalSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class BasketV4PlayerRegistrationServiceImpl implements IBasketV4PlayerRegistrationService {

    private static final Integer NOT_DELETED = 0;
    private static final int TEAM_STATUS_ACTIVE = 1;
    private static final int TEAM_AUDIT_APPROVED = 1;
    private static final int MEMBER_ACTIVE = 1;
    private static final int IS_CURRENT = 1;
    private static final int PLAYER_CERT_APPROVED = 3;
    private static final int PLATFORM_ACCOUNT_ACTIVE = 1;

    @Resource
    private BasketV4PlayerRegistrationMapper playerRegistrationMapper;
    @Resource
    private BasketCaptainGrantDao captainGrantDao;
    @Resource
    private BasketRegistrationDao registrationDao;
    @Resource
    private BasketRegistrationPlayerDao registrationPlayerDao;
    @Resource
    private BasketRegistrationAuditDao registrationAuditDao;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private CaptainGrantSupport captainGrantSupport;
    @Resource
    private TeamRegistrationEligibilitySupport eligibilitySupport;
    @Resource
    private BasketSeasonDao seasonDao;
    @Resource
    private BasketSeriesDao seriesDao;

    @Override
    public List<V4AvailableDivisionResp> listAvailableDivisions() {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        List<V4AvailableDivisionResp> rows = playerRegistrationMapper.selectAvailableDivisions(memberId, player.getId());
        Date now = new Date();
        for (V4AvailableDivisionResp row : rows) {
            enrichAvailability(row, player.getId(), now);
        }
        return rows;
    }

    @Override
    public V4RegistrationDivisionContextResp getDivisionContext(Long divisionId) {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        V4RegistrationDivisionContextResp resp = new V4RegistrationDivisionContextResp();
        resp.setDivisionId(divisionId);
        resp.setDivisionExists(false);
        resp.setCanRegister(false);
        resp.setCanViewRegistration(false);

        if (divisionId == null) {
            resp.setCannotRegisterReasonCode("DIVISION_NOT_FOUND");
            resp.setCannotRegisterReason("赛事不存在或已失效");
            resp.setRegistrationWindowStatus("CLOSED");
            resp.setRegistrationWindowStatusLabel("报名已截止");
            return resp;
        }

        BasketDivision division = divisionDao.selectById(divisionId);
        Date now = new Date();
        if (division == null || !NOT_DELETED.equals(division.getIsDelete())) {
            resp.setCannotRegisterReasonCode("DIVISION_NOT_FOUND");
            resp.setCannotRegisterReason("赛事不存在或已失效");
            resp.setRegistrationWindowStatus("CLOSED");
            resp.setRegistrationWindowStatusLabel("报名已截止");
            return resp;
        }

        resp.setDivisionExists(true);
        fillDivisionContextBase(resp, division);

        BasketCaptainGrant grant = findActiveGrant(divisionId, player.getId());
        BasketTeam team = null;
        if (grant != null && grant.getTeamId() != null) {
            team = teamDao.selectById(grant.getTeamId());
            resp.setGrantTeamId(grant.getTeamId());
            if (team != null) {
                resp.setGrantTeamName(team.getTeamName());
            }
            resp.setMyGrantStatus(eligibilitySupport.isGrantUsable(grant, now)
                    ? CaptainGrantStatusEnum.ACTIVE.getCode()
                    : grant.getGrantStatus());
        }

        BasketRegistration registration = findRegistrationByDivisionMember(divisionId, memberId);
        if (registration != null) {
            resp.setMyRegistrationStatus(registration.getRegistrationStatus());
            resp.setMyRegistrationId(registration.getId());
            resp.setParticipationRole(registration.getParticipationRole());
        }

        Integer approved = playerRegistrationMapper.countApprovedByDivisionId(divisionId);
        resp.setApprovedTeamCount(approved == null ? 0 : approved);
        if (division.getMaxTeamCount() != null) {
            resp.setMaxTeamCount(division.getMaxTeamCount());
        }
        resp.setRemainingTeamCount(null);

        if (RegistrationModeEnum.PERSONAL.getCode().equals(division.getRegistrationMode())) {
            resp.setCanRegister(false);
            resp.setCanViewRegistration(resp.getMyRegistrationId() != null);
            resp.setCannotRegisterReasonCode("DIVISION_NOT_OPEN");
            resp.setCannotRegisterReason("该 Division 为个人报名模式，请使用个人报名入口");
            resp.setRegistrationWindowStatus(eligibilitySupport.resolveWindowStatus(division, now).getCode());
            resp.setRegistrationWindowStatusLabel(eligibilitySupport.resolveWindowStatus(division, now).getLabel());
            resp.setTeamEligible(false);
            fillRosterContext(resp, division, team, player.getId());
            return resp;
        }

        TeamRegistrationEligibility eligibility = eligibilitySupport.evaluate(
                division, grant, team, resp.getMyRegistrationStatus(), resp.getRemainingTeamCount(), now);
        applyEligibility(resp, eligibility);
        resp.setTeamEligible(eligibility.isCanRegister()
                || (grant != null && team != null
                && eligibilitySupport.isGrantUsable(grant, now)
                && Objects.equals(team.getDivisionId(), division.getId())
                && Objects.equals(TEAM_STATUS_ACTIVE, team.getTeamStatus())
                && Objects.equals(TEAM_AUDIT_APPROVED, team.getAuditStatus())));
        fillRosterContext(resp, division, team, player.getId());
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveRegistration(V4RegistrationSaveReq req) {
        validateSaveReq(req);
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        BasketDivision division = requireDivision(req.getDivisionId());
        rejectPersonalModeDivision(division.getRegistrationMode());
        BasketTeam team = requireValidTeamEntity(req.getTeamId());
        if (!Objects.equals(team.getDivisionId(), req.getDivisionId())) {
            throw new ServiceException("球队所属赛事与报名 Division 不一致");
        }
        requireTeamCaptainAccess(team, memberId);
        BasketCaptainGrant grant = requireActiveGrant(req.getDivisionId(), player.getId());
        captainGrantSupport.assertTeamBindingForRegistration(grant, team);

        Date now = new Date();
        Integer remaining = null;
        BasketRegistration existingForEligibility = findRegistration(req.getDivisionId(), req.getTeamId());
        TeamRegistrationEligibility eligibility = eligibilitySupport.evaluate(
                division, grant, team,
                existingForEligibility == null ? null : existingForEligibility.getRegistrationStatus(),
                remaining, now);
        eligibilitySupport.assertCanMutateRegistration(eligibility);

        BasketRegistration registration = existingForEligibility;
        if (registration != null && RegistrationStatusEnum.APPROVED.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("报名已审核通过，不可再编辑");
        }

        // 草稿允许不足最低阵容；提交时再校验 min
        List<BasketRegistrationPlayer> lineup = buildLineup(
                division, req.getTeamId(), player.getId(), req.getPlayerIds(), false);
        if (registration == null) {
            registration = new BasketRegistration();
            registration.setSeasonId(division.getSeasonId());
            registration.setDivisionId(division.getId());
            registration.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
            registration.setRegistrationSource(com.zhs.enums.v4.RegistrationSourceEnum.TEAM_DIRECT.getCode());
            registration.setTeamId(req.getTeamId());
            registration.setCaptainPlayerId(player.getId());
            registration.setCaptainMemberId(memberId);
            registration.setRegistrationStatus(RegistrationStatusEnum.DRAFT.getCode());
            registration.setLineupCount(lineup.size());
            registration.setCreateTime(now);
            registration.setUpdateTime(now);
            registration.setIsDelete(NOT_DELETED);
            registrationDao.insert(registration);
        } else {
            if (!RegistrationStatusEnum.DRAFT.getCode().equals(registration.getRegistrationStatus())
                    && !RegistrationStatusEnum.REJECTED.getCode().equals(registration.getRegistrationStatus())
                    && !RegistrationStatusEnum.WITHDRAWN.getCode().equals(registration.getRegistrationStatus())) {
                throw new ServiceException("当前状态不可保存草稿");
            }
            registration.setLineupCount(lineup.size());
            registration.setCaptainPlayerId(player.getId());
            registration.setCaptainMemberId(memberId);
            registration.setRegistrationStatus(RegistrationStatusEnum.DRAFT.getCode());
            registration.setRejectReason(null);
            registration.setUpdateTime(now);
            registrationDao.updateById(registration);
            registrationPlayerDao.delete(new LambdaQueryWrapper<BasketRegistrationPlayer>()
                    .eq(BasketRegistrationPlayer::getRegistrationId, registration.getId()));
        }

        for (BasketRegistrationPlayer row : lineup) {
            row.setRegistrationId(registration.getId());
            registrationPlayerDao.insert(row);
        }
        return registration.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitRegistration(V4RegistrationIdReq req) {
        BasketRegistration registration = requireCaptainRegistration(req.getRegistrationId());
        if (!RegistrationStatusEnum.DRAFT.getCode().equals(registration.getRegistrationStatus())
                && !RegistrationStatusEnum.REJECTED.getCode().equals(registration.getRegistrationStatus())
                && !RegistrationStatusEnum.WITHDRAWN.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("当前状态不可提交");
        }
        BasketDivision division = requireDivision(registration.getDivisionId());
        rejectPersonalModeDivision(division.getRegistrationMode());
        BasketCaptainGrant grant = requireActiveGrant(registration.getDivisionId(), registration.getCaptainPlayerId());
        BasketTeam team = requireValidTeamEntity(registration.getTeamId());
        Date eligibilityNow = new Date();
        Integer remainingCount = null;
        TeamRegistrationEligibility submitEligibility = eligibilitySupport.evaluate(
                division, grant, team, registration.getRegistrationStatus(), remainingCount, eligibilityNow);
        // 驳回/草稿重提：窗口关闭后不允许再 mutate（与列表 canRegister 一致）
        if (!RegistrationStatusEnum.PENDING_REVIEW.getCode().equals(registration.getRegistrationStatus())
                && !RegistrationStatusEnum.APPROVED.getCode().equals(registration.getRegistrationStatus())) {
            eligibilitySupport.assertCanMutateRegistration(submitEligibility);
        }
        List<BasketRegistrationPlayer> players = registrationPlayerDao.selectList(new LambdaQueryWrapper<BasketRegistrationPlayer>()
                .eq(BasketRegistrationPlayer::getRegistrationId, registration.getId())
                .eq(BasketRegistrationPlayer::getIsDelete, NOT_DELETED));
        if (players.isEmpty()) {
            throw new ServiceException("请先保存报名阵容");
        }
        List<Long> playerIds = new ArrayList<>();
        for (BasketRegistrationPlayer row : players) {
            playerIds.add(row.getPlayerId());
        }
        // 提交前重新执行阵容资格校验（含 min/max、队长、平台身份）
        buildLineup(division, registration.getTeamId(), registration.getCaptainPlayerId(), playerIds, true);

        Date now = new Date();
        String beforeStatus = registration.getRegistrationStatus();
        registration.setRegistrationStatus(RegistrationStatusEnum.PENDING_REVIEW.getCode());
        registration.setSubmitTime(now);
        registration.setWithdrawTime(null);
        registration.setUpdateTime(now);
        registrationDao.updateById(registration);
        writeAudit(registration, "SUBMIT", beforeStatus, RegistrationStatusEnum.PENDING_REVIEW.getCode(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawRegistration(V4RegistrationIdReq req) {
        BasketRegistration registration = requireCaptainRegistration(req.getRegistrationId());
        if (!RegistrationStatusEnum.PENDING_REVIEW.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("仅待审核状态可撤回");
        }
        Date now = new Date();
        String beforeStatus = registration.getRegistrationStatus();
        registration.setRegistrationStatus(RegistrationStatusEnum.WITHDRAWN.getCode());
        registration.setWithdrawTime(now);
        registration.setUpdateTime(now);
        registrationDao.updateById(registration);
        writeAudit(registration, "WITHDRAW", beforeStatus, RegistrationStatusEnum.WITHDRAWN.getCode(), null, null);
    }

    @Override
    public List<V4MyRegistrationResp> listMyRegistrations() {
        return playerRegistrationMapper.selectMyRegistrations(UserContext.getRequiredMemberId());
    }

    @Override
    public V4RegistrationDetailResp getRegistrationDetail(Long registrationId) {
        BasketRegistration registration = requireCaptainRegistration(registrationId);
        V4RegistrationDetailResp resp = new V4RegistrationDetailResp();
        List<V4MyRegistrationResp> mine = playerRegistrationMapper.selectMyRegistrations(registration.getCaptainMemberId());
        V4MyRegistrationResp base = mine.stream()
                .filter(r -> registrationId.equals(r.getRegistrationId()))
                .findFirst()
                .orElse(null);
        if (base != null) {
            org.springframework.beans.BeanUtils.copyProperties(base, resp);
        } else {
            resp.setRegistrationId(registration.getId());
            resp.setSeasonId(registration.getSeasonId());
            resp.setDivisionId(registration.getDivisionId());
            resp.setTeamId(registration.getTeamId());
            resp.setRegistrationStatus(registration.getRegistrationStatus());
            resp.setLineupCount(registration.getLineupCount());
            resp.setSubmitTime(registration.getSubmitTime());
            resp.setApproveTime(registration.getApproveTime());
            resp.setRejectReason(registration.getRejectReason());
        }
        List<V4RegistrationPlayerItemResp> players = new ArrayList<>();
        for (BasketRegistrationPlayer p : registrationPlayerDao.selectList(new LambdaQueryWrapper<BasketRegistrationPlayer>()
                .eq(BasketRegistrationPlayer::getRegistrationId, registrationId)
                .eq(BasketRegistrationPlayer::getIsDelete, NOT_DELETED)
                .orderByAsc(BasketRegistrationPlayer::getSortOrder))) {
            V4RegistrationPlayerItemResp item = new V4RegistrationPlayerItemResp();
            item.setPlayerId(p.getPlayerId());
            item.setPlayerName(p.getPlayerNameSnapshot());
            item.setIsCaptain(p.getIsCaptain());
            item.setSortOrder(p.getSortOrder());
            players.add(item);
        }
        resp.setPlayers(players);
        return resp;
    }

    private void enrichAvailability(V4AvailableDivisionResp row, Long playerId, Date now) {
        BasketDivision division = new BasketDivision();
        division.setId(row.getDivisionId());
        division.setSeasonId(row.getSeasonId());
        division.setDivisionStatus(row.getDivisionStatus());
        division.setRegistrationStartTime(row.getRegistrationStartTime());
        division.setRegistrationEndTime(row.getRegistrationEndTime());
        division.setIsDelete(NOT_DELETED);

        BasketCaptainGrant grant = findActiveGrant(row.getDivisionId(), playerId);
        BasketTeam team = null;
        if (row.getGrantTeamId() != null) {
            team = teamDao.selectById(row.getGrantTeamId());
        } else if (grant != null && grant.getTeamId() != null) {
            team = teamDao.selectById(grant.getTeamId());
            row.setGrantTeamId(grant.getTeamId());
            if (team != null) {
                row.setGrantTeamName(team.getTeamName());
            }
        }
        if (grant != null && eligibilitySupport.isGrantUsable(grant, now)) {
            row.setMyGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        } else if (grant != null) {
            row.setMyGrantStatus(grant.getGrantStatus());
        }

        row.setRemainingTeamCount(null);
        TeamRegistrationEligibility eligibility = eligibilitySupport.evaluate(
                division, grant, team, row.getMyRegistrationStatus(), null, now);
        applyEligibility(row, eligibility);
    }

    private void applyEligibility(V4AvailableDivisionResp row, TeamRegistrationEligibility eligibility) {
        row.setCanRegister(eligibility.isCanRegister());
        row.setCanViewRegistration(eligibility.isCanViewRegistration());
        row.setRegistrationWindowStatus(eligibility.getWindowStatusCode());
        row.setRegistrationWindowStatusLabel(eligibility.getWindowStatusLabel());
        row.setCannotRegisterReasonCode(eligibility.getReasonCode());
        row.setCannotRegisterReason(eligibility.isCanRegister() ? null : eligibility.getReasonMessage());
    }

    private void applyEligibility(V4RegistrationDivisionContextResp resp, TeamRegistrationEligibility eligibility) {
        resp.setCanRegister(eligibility.isCanRegister());
        resp.setCanViewRegistration(eligibility.isCanViewRegistration());
        resp.setRegistrationWindowStatus(eligibility.getWindowStatusCode());
        resp.setRegistrationWindowStatusLabel(eligibility.getWindowStatusLabel());
        resp.setCannotRegisterReasonCode(eligibility.getReasonCode());
        resp.setCannotRegisterReason(eligibility.isCanRegister() ? null : eligibility.getReasonMessage());
    }

    private void fillDivisionContextBase(V4RegistrationDivisionContextResp resp, BasketDivision division) {
        resp.setSeasonId(division.getSeasonId());
        resp.setDivisionName(division.getDivisionName());
        resp.setPlatformCode(division.getPlatformCode());
        resp.setRegistrationMode(division.getRegistrationMode());
        resp.setGameMode(division.getGameMode());
        resp.setDivisionStatus(division.getDivisionStatus());
        resp.setLineupMinCount(division.getLineupMinCount());
        resp.setLineupMaxCount(division.getLineupMaxCount());
        resp.setRegistrationStartTime(division.getRegistrationStartTime());
        resp.setRegistrationEndTime(division.getRegistrationEndTime());
        resp.setMatchStartTime(division.getMatchStartTime());
        resp.setMatchEndTime(division.getMatchEndTime());
        resp.setMaxTeamCount(division.getMaxTeamCount());
        BasketSeason season = seasonDao.selectById(division.getSeasonId());
        if (season != null) {
            resp.setSeasonTitle(season.getDisplayTitle());
            BasketSeries series = seriesDao.selectById(season.getSeriesId());
            if (series != null) {
                resp.setSeriesName(series.getSeriesName());
            }
        }
    }

    private BasketRegistration findRegistrationByDivisionMember(Long divisionId, Long memberId) {
        return registrationDao.selectOne(new LambdaQueryWrapper<BasketRegistration>()
                .eq(BasketRegistration::getDivisionId, divisionId)
                .eq(BasketRegistration::getCaptainMemberId, memberId)
                .eq(BasketRegistration::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private BasketCaptainGrant findActiveGrant(Long divisionId, Long playerId) {
        return captainGrantSupport.findActiveGrantByDivisionPlayer(divisionId, playerId);
    }

    private BasketCaptainGrant requireActiveGrant(Long divisionId, Long playerId) {
        BasketCaptainGrant grant = captainGrantSupport.findActiveGrantByDivisionPlayer(divisionId, playerId);
        if (!isGrantUsable(grant, new Date())) {
            throw new ServiceException("无有效队长授权");
        }
        return grant;
    }

    private void requireTeamCaptainAccess(BasketTeam team, Long memberId) {
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        if (!memberId.equals(team.getCaptainMemberId())) {
            throw new ServiceException("无权操作该球队");
        }
    }

    private boolean isGrantUsable(BasketCaptainGrant grant, Date now) {
        return eligibilitySupport.isGrantUsable(grant, now);
    }

    private BasketRegistration findRegistration(Long divisionId, Long teamId) {
        return registrationDao.selectOne(new LambdaQueryWrapper<BasketRegistration>()
                .eq(BasketRegistration::getDivisionId, divisionId)
                .eq(BasketRegistration::getTeamId, teamId)
                .eq(BasketRegistration::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private BasketRegistration requireCaptainRegistration(Long registrationId) {
        if (registrationId == null) {
            throw new ServiceException("registrationId 不能为空");
        }
        Long memberId = UserContext.getRequiredMemberId();
        BasketRegistration registration = registrationDao.selectById(registrationId);
        if (registration == null || !NOT_DELETED.equals(registration.getIsDelete())) {
            throw new ServiceException("报名不存在");
        }
        if (!memberId.equals(registration.getCaptainMemberId())) {
            throw new ServiceException("无权操作该报名");
        }
        return registration;
    }

    private List<BasketRegistrationPlayer> buildLineup(BasketDivision division,
                                                       Long teamId,
                                                       Long captainPlayerId,
                                                       List<Long> playerIds,
                                                       boolean enforceMinRoster) {
        if (CollectionUtils.isEmpty(playerIds)) {
            throw new ServiceException("阵容不能为空");
        }
        Set<Long> unique = new HashSet<>(playerIds);
        if (unique.size() != playerIds.size()) {
            throw new ServiceException("阵容中存在重复球员");
        }
        if (!unique.contains(captainPlayerId)) {
            throw new ServiceException("队长必须在报名阵容中");
        }
        int min = division.getLineupMinCount() != null ? division.getLineupMinCount() : GameModeEnum.FIVE_V_FIVE.getMinRosterCount();
        int max = division.getLineupMaxCount() != null ? division.getLineupMaxCount() : GameModeEnum.FIVE_V_FIVE.getMaxRosterCount();
        if (enforceMinRoster && playerIds.size() < min) {
            throw new ServiceException("阵容人数不能少于 " + min + " 人");
        }
        if (playerIds.size() > max) {
            throw new ServiceException("阵容人数不能多于 " + max + " 人");
        }

        String platformCode = division.getPlatformCode();
        List<BasketRegistrationPlayer> rows = new ArrayList<>();
        Date now = new Date();
        int sort = 0;
        for (Long playerId : playerIds) {
            requireTeamMember(teamId, playerId);
            BasketPlayerProfile profile = requireCertifiedPlayer(playerId);
            BasketPlayerPlatformAccount account = requirePlatformIdentity(playerId, platformCode);
            BasketRegistrationPlayer row = new BasketRegistrationPlayer();
            row.setDivisionId(division.getId());
            row.setSeasonId(division.getSeasonId());
            row.setTeamId(teamId);
            row.setPlayerId(playerId);
            row.setMemberId(profile.getMemberId());
            row.setPlatformIdentityId(account.getId());
            row.setPlatformCode(normalizePlatformCode(account.getPlatform()));
            row.setGameIdSnapshot(account.getDisplayGameId());
            row.setPlayerNameSnapshot(profile.getGameNickname());
            row.setPosition(profile.getPositionCode());
            row.setJerseyNo(null);
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

    private void fillRosterContext(V4RegistrationDivisionContextResp resp,
                                   BasketDivision division,
                                   BasketTeam team,
                                   Long captainPlayerId) {
        int min = division.getLineupMinCount() != null
                ? division.getLineupMinCount()
                : GameModeEnum.FIVE_V_FIVE.getMinRosterCount();
        List<V4RegistrationRosterPlayerResp> eligible = new ArrayList<>();
        List<V4RegistrationRosterPlayerResp> ineligible = new ArrayList<>();
        if (team == null) {
            resp.setTeamMemberCount(0);
            resp.setEligibleRosterCount(0);
            resp.setIneligibleRosterCount(0);
            resp.setEligiblePlayers(eligible);
            resp.setIneligiblePlayers(ineligible);
            resp.setCanSubmitRegistration(false);
            return;
        }

        List<BasketTeamMember> members = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, team.getId())
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, MEMBER_ACTIVE)
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .orderByAsc(BasketTeamMember::getId));
        for (BasketTeamMember member : members) {
            V4RegistrationRosterPlayerResp item = evaluateRosterPlayer(division, member, captainPlayerId);
            if (Boolean.TRUE.equals(item.getEligible())) {
                eligible.add(item);
            } else {
                ineligible.add(item);
            }
        }
        resp.setTeamMemberCount(members.size());
        resp.setEligibleRosterCount(eligible.size());
        resp.setIneligibleRosterCount(ineligible.size());
        resp.setEligiblePlayers(eligible);
        resp.setIneligiblePlayers(ineligible);
        resp.setCanSubmitRegistration(Boolean.TRUE.equals(resp.getCanRegister()) && eligible.size() >= min);
    }

    private V4RegistrationRosterPlayerResp evaluateRosterPlayer(BasketDivision division,
                                                                BasketTeamMember member,
                                                                Long captainPlayerId) {
        V4RegistrationRosterPlayerResp item = new V4RegistrationRosterPlayerResp();
        item.setTeamMemberId(member.getId());
        item.setPlayerId(member.getPlayerId());
        item.setMemberId(member.getMemberId());
        item.setRoleType(member.getRoleType());
        item.setRoleTypeText(roleTypeText(member.getRoleType()));
        item.setCaptain(captainPlayerId != null && captainPlayerId.equals(member.getPlayerId()));
        item.setPlatformCode(normalizePlatformCode(division.getPlatformCode()));
        item.setEligible(false);

        if (member.getPlayerId() == null) {
            item.setReasonCode("PLAYER_MISSING");
            item.setReasonMessage("成员未关联球员档案");
            item.setDisplayName("未知球员");
            return item;
        }

        BasketPlayerProfile profile = playerProfileDao.selectById(member.getPlayerId());
        if (profile == null || !NOT_DELETED.equals(profile.getIsDelete())) {
            item.setReasonCode("PLAYER_NOT_FOUND");
            item.setReasonMessage("球员档案不存在");
            item.setDisplayName("未知球员");
            return item;
        }
        item.setPlayerNo(profile.getPlayerNo());
        item.setDisplayName(resolveDisplayName(profile, member.getMemberId()));

        if (!Integer.valueOf(PLAYER_CERT_APPROVED).equals(profile.getCertificationStatus())) {
            item.setReasonCode("PLAYER_NOT_CERTIFIED");
            item.setReasonMessage("球员尚未完成认证");
            item.setPlatformIdentityStatus("MISSING");
            return item;
        }

        BasketPlayerPlatformAccount account = findPlatformIdentity(member.getPlayerId(), division.getPlatformCode());
        if (account == null) {
            item.setReasonCode("PLATFORM_IDENTITY_MISSING");
            item.setReasonMessage("未绑定或未通过 "
                    + GameTerminalSupport.formatDisplayName(division.getPlatformCode())
                    + " 游戏身份认证");
            item.setPlatformIdentityStatus("MISSING");
            return item;
        }
        item.setPlatformGameId(account.getDisplayGameId());
        item.setPlatformIdentityStatus("ACTIVE");
        if (!StringUtils.hasText(account.getDisplayGameId())) {
            item.setReasonCode("GAME_ID_MISSING");
            item.setReasonMessage("平台游戏 ID 无效");
            return item;
        }

        item.setEligible(true);
        item.setReasonCode(null);
        item.setReasonMessage(null);
        return item;
    }

    private BasketPlayerPlatformAccount findPlatformIdentity(Long playerId, String divisionPlatformCode) {
        List<BasketPlayerPlatformAccount> accounts = platformAccountDao.selectList(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .eq(BasketPlayerPlatformAccount::getPlayerId, playerId)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED));
        for (BasketPlayerPlatformAccount account : accounts) {
            if (Integer.valueOf(PLATFORM_ACCOUNT_ACTIVE).equals(account.getAccountStatus())
                    && GameTerminalSupport.matchesDivisionPlatform(divisionPlatformCode, account.getPlatform())) {
                return account;
            }
        }
        return null;
    }

    private String resolveDisplayName(BasketPlayerProfile profile, Long memberId) {
        if (StringUtils.hasText(profile.getGameNickname())) {
            return profile.getGameNickname();
        }
        if (memberId != null) {
            BasketMemberProfile memberProfile = memberProfileDao.selectOne(new LambdaQueryWrapper<BasketMemberProfile>()
                    .eq(BasketMemberProfile::getMemberId, memberId)
                    .eq(BasketMemberProfile::getIsDelete, NOT_DELETED)
                    .last("limit 1"));
            if (memberProfile != null && StringUtils.hasText(memberProfile.getNickname())) {
                return memberProfile.getNickname();
            }
        }
        return profile.getPlayerNo();
    }

    private String roleTypeText(String roleType) {
        if ("CAPTAIN".equalsIgnoreCase(roleType)) {
            return "队长";
        }
        if ("VICE_CAPTAIN".equalsIgnoreCase(roleType)) {
            return "副队长";
        }
        return "队员";
    }

    private void writeAudit(BasketRegistration registration, String action, String beforeStatus, String afterStatus,
                            String auditResult, String remark) {
        Date now = new Date();
        BasketRegistrationAudit audit = new BasketRegistrationAudit();
        audit.setRegistrationId(registration.getId());
        audit.setDivisionId(registration.getDivisionId());
        audit.setSeasonId(registration.getSeasonId());
        audit.setAuditAction(action);
        audit.setAuditResult(auditResult);
        audit.setBeforeStatus(beforeStatus);
        audit.setAfterStatus(afterStatus);
        audit.setAuditUserId(UserContext.getRequiredMemberId());
        audit.setAuditTime(now);
        audit.setAuditRemark(remark);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        registrationAuditDao.insert(audit);
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

    private BasketDivision requireDivision(Long divisionId) {
        BasketDivision division = divisionDao.selectById(divisionId);
        if (division == null || !NOT_DELETED.equals(division.getIsDelete())) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }

    private BasketTeam requireValidTeamEntity(Long teamId) {
        BasketTeam team = teamDao.selectById(teamId);
        if (team == null || !NOT_DELETED.equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        if (team.getDivisionId() == null) {
            throw new ServiceException("历史球队不能直接报名新赛事，请在当前赛事重新创建球队");
        }
        if (!Integer.valueOf(TEAM_STATUS_ACTIVE).equals(team.getTeamStatus())) {
            throw new ServiceException("球队状态无效");
        }
        if (!Integer.valueOf(TEAM_AUDIT_APPROVED).equals(team.getAuditStatus())) {
            throw new ServiceException("球队尚未审核通过");
        }
        return team;
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

    private BasketPlayerPlatformAccount requirePlatformIdentity(Long playerId, String divisionPlatformCode) {
        BasketPlayerPlatformAccount account = findPlatformIdentity(playerId, divisionPlatformCode);
        if (account == null) {
            throw new ServiceException("球员缺少对应平台身份：" + GameTerminalSupport.formatDisplayName(divisionPlatformCode));
        }
        return account;
    }

    private void validateSaveReq(V4RegistrationSaveReq req) {
        if (req == null || req.getDivisionId() == null || req.getTeamId() == null) {
            throw new ServiceException("divisionId / teamId 不能为空");
        }
    }

    private void rejectPersonalModeDivision(String mode) {
        if (RegistrationModeEnum.PERSONAL.getCode().equals(mode)) {
            throw new ServiceException("该 Division 为个人报名模式，请使用个人报名入口");
        }
    }

    private String normalizePlatformCode(String platformCode) {
        return GameTerminalSupport.normalize(platformCode);
    }
}
