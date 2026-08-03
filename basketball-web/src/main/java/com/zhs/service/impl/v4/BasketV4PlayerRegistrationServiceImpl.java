package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.v4.*;
import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.enums.v4.RegistrationModeEnum;
import com.zhs.enums.v4.RegistrationStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.mapper.v4.BasketV4PlayerRegistrationMapper;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.*;
import com.zhs.request.v4.V4RegistrationIdReq;
import com.zhs.request.v4.V4RegistrationSaveReq;
import com.zhs.response.v4.V4AvailableDivisionResp;
import com.zhs.response.v4.V4MyRegistrationResp;
import com.zhs.service.v4.IBasketV4PlayerRegistrationService;
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
    @Transactional(rollbackFor = Exception.class)
    public Long saveRegistration(V4RegistrationSaveReq req) {
        validateSaveReq(req);
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        BasketDivision division = requireDivision(req.getDivisionId());
        rejectPersonalMode(division.getRegistrationMode());
        BasketCaptainGrant grant = requireActiveGrant(req.getDivisionId(), req.getTeamId(), player.getId());
        if (!req.getTeamId().equals(grant.getTeamId())) {
            throw new ServiceException("teamId 必须与队长授权一致");
        }
        requireValidTeam(req.getTeamId());

        BasketRegistration registration = findRegistration(req.getDivisionId(), req.getTeamId());
        if (registration != null && RegistrationStatusEnum.APPROVED.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("报名已审核通过，不可再编辑");
        }

        List<BasketRegistrationPlayer> lineup = buildLineup(division, req.getTeamId(), player.getId(), req.getPlayerIds());
        Date now = new Date();
        if (registration == null) {
            registration = new BasketRegistration();
            registration.setSeasonId(division.getSeasonId());
            registration.setDivisionId(division.getId());
            registration.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
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
        requireActiveGrant(registration.getDivisionId(), registration.getTeamId(), registration.getCaptainPlayerId());
        List<BasketRegistrationPlayer> players = registrationPlayerDao.selectList(new LambdaQueryWrapper<BasketRegistrationPlayer>()
                .eq(BasketRegistrationPlayer::getRegistrationId, registration.getId())
                .eq(BasketRegistrationPlayer::getIsDelete, NOT_DELETED));
        if (players.isEmpty()) {
            throw new ServiceException("请先保存报名阵容");
        }

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

    private void enrichAvailability(V4AvailableDivisionResp row, Long playerId, Date now) {
        row.setCanRegister(false);
        if (row.getRegistrationStartTime() != null && row.getRegistrationStartTime().after(now)) {
            row.setCannotRegisterReason("报名尚未开始");
            return;
        }
        if (row.getRegistrationEndTime() != null && row.getRegistrationEndTime().before(now)) {
            row.setCannotRegisterReason("报名已结束");
            return;
        }
        if (row.getRemainingTeamCount() != null && row.getRemainingTeamCount() <= 0) {
            row.setCannotRegisterReason("名额已满");
            return;
        }
        BasketCaptainGrant grant = findActiveGrant(row.getDivisionId(), playerId);
        if (grant == null || !isGrantUsable(grant, now)) {
            row.setCannotRegisterReason("无有效队长授权");
            row.setMyGrantStatus(grant == null ? null : grant.getGrantStatus());
            return;
        }
        row.setMyGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        if (RegistrationStatusEnum.PENDING_REVIEW.getCode().equals(row.getMyRegistrationStatus())) {
            row.setCannotRegisterReason("已有审核中的报名");
            return;
        }
        if (RegistrationStatusEnum.APPROVED.getCode().equals(row.getMyRegistrationStatus())) {
            row.setCannotRegisterReason("已完成报名");
            return;
        }
        row.setCanRegister(true);
        row.setCannotRegisterReason(null);
    }

    private BasketCaptainGrant findActiveGrant(Long divisionId, Long playerId) {
        return captainGrantDao.selectOne(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getDivisionId, divisionId)
                .eq(BasketCaptainGrant::getPlayerId, playerId)
                .eq(BasketCaptainGrant::getGrantStatus, CaptainGrantStatusEnum.ACTIVE.getCode())
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNotNull(BasketCaptainGrant::getActiveUniqueKey)
                .last("limit 1"));
    }

    private BasketCaptainGrant requireActiveGrant(Long divisionId, Long teamId, Long playerId) {
        BasketCaptainGrant grant = captainGrantDao.selectOne(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getDivisionId, divisionId)
                .eq(BasketCaptainGrant::getTeamId, teamId)
                .eq(BasketCaptainGrant::getPlayerId, playerId)
                .eq(BasketCaptainGrant::getGrantStatus, CaptainGrantStatusEnum.ACTIVE.getCode())
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNotNull(BasketCaptainGrant::getActiveUniqueKey)
                .last("limit 1"));
        if (!isGrantUsable(grant, new Date())) {
            throw new ServiceException("无有效队长授权");
        }
        return grant;
    }

    private boolean isGrantUsable(BasketCaptainGrant grant, Date now) {
        if (grant == null) {
            return false;
        }
        if (grant.getValidFrom() != null && grant.getValidFrom().after(now)) {
            return false;
        }
        return grant.getValidTo() == null || !grant.getValidTo().before(now);
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

    private List<BasketRegistrationPlayer> buildLineup(BasketDivision division, Long teamId, Long captainPlayerId, List<Long> playerIds) {
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
        int min = division.getLineupMinCount() != null ? division.getLineupMinCount() : 5;
        int max = division.getLineupMaxCount() != null ? division.getLineupMaxCount() : 8;
        if (playerIds.size() < min) {
            throw new ServiceException("阵容人数不能少于 " + min + " 人");
        }
        if (playerIds.size() > max) {
            throw new ServiceException("阵容人数不能多于 " + max + " 人");
        }

        String platformCode = normalizePlatformCode(division.getPlatformCode());
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
            row.setPlatformCode(platformCode);
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

    private BasketPlayerPlatformAccount requirePlatformIdentity(Long playerId, String platformCode) {
        List<BasketPlayerPlatformAccount> accounts = platformAccountDao.selectList(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .eq(BasketPlayerPlatformAccount::getPlayerId, playerId)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED));
        for (BasketPlayerPlatformAccount account : accounts) {
            if (Integer.valueOf(PLATFORM_ACCOUNT_ACTIVE).equals(account.getAccountStatus())
                    && platformCode.equals(normalizePlatformCode(account.getPlatform()))) {
                return account;
            }
        }
        throw new ServiceException("球员缺少对应平台身份：" + platformCode);
    }

    private void validateSaveReq(V4RegistrationSaveReq req) {
        if (req == null || req.getDivisionId() == null || req.getTeamId() == null) {
            throw new ServiceException("divisionId / teamId 不能为空");
        }
    }

    private void rejectPersonalMode(String mode) {
        if (RegistrationModeEnum.PERSONAL.getCode().equals(mode)) {
            throw new ServiceException("PERSONAL 个人报名暂未开放");
        }
    }

    private String normalizePlatformCode(String platformCode) {
        return GameTerminalSupport.normalize(platformCode);
    }
}
