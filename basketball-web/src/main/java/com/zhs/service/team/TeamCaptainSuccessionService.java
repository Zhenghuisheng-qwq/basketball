package com.zhs.service.team;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.v4.BasketCaptainGrantDao;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.enums.TeamMemberRoleEnum;
import com.zhs.enums.TeamMembershipStatusEnum;
import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.v4.BasketCaptainGrant;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.support.CaptainGrantSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 队长席位和本赛事授权一起交接。原队长留在队里，仍不能被其他队伍邀请。
 * 解散只提交申请，管理员审核通过后才结束全队成员关系。
 */
@Service
public class TeamCaptainSuccessionService {

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;

    @Resource
    private ICaptainPermissionService captainPermissionService;
    @Resource
    private BasketTeamDao teamDao;
    @Resource
    private BasketTeamMemberDao teamMemberDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketCaptainGrantDao captainGrantDao;
    @Resource
    private CaptainGrantSupport captainGrantSupport;
    @Resource
    private TeamMembershipChangeGate membershipChangeGate;

    @Transactional(rollbackFor = Exception.class)
    public void transfer(String teamId, String targetTeamMemberId) {
        Long teamPk = parseId(teamId, "球队主键");
        Long targetPk = parseId(targetTeamMemberId, "接手成员");
        Long operatorId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(operatorId, teamPk);
        BasketTeamMember target = requireCurrentMember(teamPk, targetPk);
        if (Objects.equals(team.getCaptainMemberId(), target.getMemberId())) {
            throw new ServiceException("该队员已经是队长");
        }
        BasketPlayerProfile targetProfile = playerProfileDao.selectById(target.getPlayerId());
        if (targetProfile == null || !NOT_DELETED.equals(targetProfile.getIsDelete())) {
            throw new ServiceException("接手球员不存在");
        }
        if (!Objects.equals(PlayerCertStatusEnum.APPROVED.getCode(), targetProfile.getCertificationStatus())) {
            throw new ServiceException("接手人尚未认证通过");
        }
        BasketPlayerProfile operatorProfile = captainPermissionService.requireCertifiedPlayer(operatorId);
        BasketCaptainGrant sourceGrant = captainGrantSupport.findActiveGrantForDivision(
                operatorId, team.getDivisionId(), operatorProfile.getId());
        if (sourceGrant == null) {
            throw new ServiceException(CaptainGrantSupport.NO_ACTIVE_GRANT_MESSAGE, 403);
        }
        BasketCaptainGrant targetGrant = captainGrantSupport.findActiveGrantByDivisionPlayer(
                team.getDivisionId(), target.getPlayerId());
        if (targetGrant != null && targetGrant.getTeamId() != null && !Objects.equals(targetGrant.getTeamId(), team.getId())) {
            throw new ServiceException("接手人在本赛事的队长授权已绑定其他球队");
        }
        Long keepGrantId = targetGrant == null ? null : targetGrant.getId();
        revokeOtherActiveGrants(team.getId(), keepGrantId);
        if (sourceGrant.getId() != null && !Objects.equals(sourceGrant.getId(), keepGrantId)) {
            revokeGrant(sourceGrant);
        }
        if (targetGrant == null) {
            insertGrant(sourceGrant, team, target);
        } else if (targetGrant.getTeamId() == null) {
            captainGrantSupport.bindGrantToTeam(targetGrant, team.getId());
        }
        demoteOtherCaptains(team.getId(), target.getId());
        teamMemberDao.update(null, new LambdaUpdateWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getId, target.getId())
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .set(BasketTeamMember::getRoleType, TeamMemberRoleEnum.CAPTAIN.getCode())
                .set(BasketTeamMember::getUpdateTime, new Date()));
        teamDao.update(null, new LambdaUpdateWrapper<BasketTeam>()
                .eq(BasketTeam::getId, team.getId())
                .set(BasketTeam::getCaptainMemberId, target.getMemberId())
                .set(BasketTeam::getCaptainPlayerId, target.getPlayerId())
                .set(BasketTeam::getUpdateTime, new Date()));
        String name = targetProfile.getGameNickname() == null ? targetProfile.getPlayerNo() : targetProfile.getGameNickname();
        membershipChangeGate.recordCaptainTransfer(team, operatorId, target.getPlayerId(), target.getMemberId(),
                "队长移交给 " + (name == null ? "接手队员" : name));
    }

    @Transactional(rollbackFor = Exception.class)
    public void applyDissolve(String teamId, String reason) {
        Long teamPk = parseId(teamId, "球队主键");
        Long operatorId = UserContext.getRequiredUserId();
        BasketTeam team = captainPermissionService.requireTeamCaptain(operatorId, teamPk);
        BasketTeamMember seat = requireCurrentByMember(teamPk, operatorId);
        if (seat.getPlayerId() == null) {
            throw new ServiceException("队长球员资料不存在，不能提交解散");
        }
        membershipChangeGate.submitDissolve(team, operatorId, seat.getPlayerId(), reason);
    }

    private void revokeOtherActiveGrants(Long teamId, Long keepGrantId) {
        List<BasketCaptainGrant> grants = captainGrantDao.selectList(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getTeamId, teamId)
                .eq(BasketCaptainGrant::getGrantStatus, CaptainGrantStatusEnum.ACTIVE.getCode())
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED));
        for (BasketCaptainGrant grant : grants) {
            if (keepGrantId != null && keepGrantId.equals(grant.getId())) {
                continue;
            }
            revokeGrant(grant);
        }
    }

    private void revokeGrant(BasketCaptainGrant grant) {
        int updated = captainGrantDao.update(null, new LambdaUpdateWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getId, grant.getId())
                .eq(BasketCaptainGrant::getGrantStatus, CaptainGrantStatusEnum.ACTIVE.getCode())
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .set(BasketCaptainGrant::getGrantStatus, CaptainGrantStatusEnum.REVOKED.getCode())
                .set(BasketCaptainGrant::getActiveUniqueKey, null)
                .set(BasketCaptainGrant::getUpdateTime, new Date()));
        if (updated == 1) {
            return;
        }
        BasketCaptainGrant current = captainGrantDao.selectById(grant.getId());
        if (current == null || !CaptainGrantStatusEnum.ACTIVE.getCode().equals(current.getGrantStatus())) {
            return;
        }
        throw new ServiceException("原队长授权状态已变化，请刷新后重试");
    }

    private void insertGrant(BasketCaptainGrant source, BasketTeam team, BasketTeamMember target) {
        Date now = new Date();
        BasketCaptainGrant created = new BasketCaptainGrant();
        if (source.getSeasonId() == null) {
            throw new ServiceException("原队长授权缺少赛季，不能移交");
        }
        created.setDivisionId(team.getDivisionId());
        created.setSeasonId(source.getSeasonId());
        created.setTeamId(team.getId());
        created.setPlayerId(target.getPlayerId());
        created.setMemberId(target.getMemberId());
        created.setGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
        created.setActiveUniqueKey(CaptainGrantSupport.buildActiveUniqueKey(team.getDivisionId(), target.getPlayerId()));
        created.setValidFrom(source.getValidFrom() == null ? now : source.getValidFrom());
        created.setValidTo(source.getValidTo());
        created.setValidToMode(source.getValidToMode());
        created.setRemark("队长移交");
        created.setGrantSource("MANUAL");
        created.setCreateTime(now);
        created.setUpdateTime(now);
        created.setIsDelete(NOT_DELETED);
        captainGrantDao.insert(created);
    }

    private void demoteOtherCaptains(Long teamId, Long keepRowId) {
        teamMemberDao.update(null, new LambdaUpdateWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .eq(BasketTeamMember::getRoleType, TeamMemberRoleEnum.CAPTAIN.getCode())
                .ne(BasketTeamMember::getId, keepRowId)
                .set(BasketTeamMember::getRoleType, TeamMemberRoleEnum.MEMBER.getCode())
                .set(BasketTeamMember::getUpdateTime, new Date()));
    }

    private BasketTeamMember requireCurrentMember(Long teamId, Long teamMemberId) {
        BasketTeamMember member = teamMemberDao.selectById(teamMemberId);
        if (!currentOnTeam(member, teamId)) {
            throw new ServiceException("接手人不是这支球队的当前成员");
        }
        return member;
    }

    private BasketTeamMember requireCurrentByMember(Long teamId, Long memberId) {
        List<BasketTeamMember> rows = teamMemberDao.selectList(new LambdaQueryWrapper<BasketTeamMember>()
                .eq(BasketTeamMember::getTeamId, teamId)
                .eq(BasketTeamMember::getMemberId, memberId)
                .eq(BasketTeamMember::getIsCurrent, IS_CURRENT)
                .eq(BasketTeamMember::getMembershipStatus, TeamMembershipStatusEnum.ACTIVE.getCode())
                .eq(BasketTeamMember::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (rows.isEmpty()) {
            throw new ServiceException("你不是这支球队的当前成员");
        }
        return rows.get(0);
    }

    private boolean currentOnTeam(BasketTeamMember member, Long teamId) {
        return member != null
                && NOT_DELETED.equals(member.getIsDelete())
                && Objects.equals(member.getTeamId(), teamId)
                && Objects.equals(member.getIsCurrent(), IS_CURRENT)
                && Objects.equals(member.getMembershipStatus(), TeamMembershipStatusEnum.ACTIVE.getCode());
    }

    private Long parseId(String raw, String label) {
        if (raw == null || raw.isBlank()) {
            throw new ServiceException(label + "不能为空");
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(label + "不正确");
        }
    }
}
