package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.BasketTeamMember;
import com.zhs.basket.domain.BasketTeamMembershipChange;
import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.mapper.BasketPlayerPlatformAccountMapper;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.BasketTeamMemberMapper;
import com.zhs.basket.mapper.BasketTeamMembershipChangeMapper;
import com.zhs.basket.enums.CaptainGrantLogActionTypeEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.enums.v4.CaptainGrantValidToModeEnum;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.support.CaptainGrantLogSupport;
import com.zhs.basket.support.v4.CaptainGrantActiveKeySupport;
import com.zhs.basket.support.v4.CaptainGrantRuntimeSupport;
import com.zhs.basket.req.TeamMembershipChangeRejectReq;
import com.zhs.basket.query.TeamMembershipChangeQuery;
import com.zhs.basket.service.IBasketTeamMembershipChangeService;
import com.zhs.basket.support.PlatformGameIdDisplay;
import com.zhs.basket.support.v4.MembershipRosterApplier;
import com.zhs.basket.req.TeamCaptainTransferReq;
import com.zhs.basket.req.TeamDissolveSubmitReq;
import com.zhs.basket.vo.MembershipRosterEffect;
import com.zhs.basket.vo.TeamCaptainDeskVo;
import com.zhs.basket.vo.TeamMembershipChangeDetailVo;
import com.zhs.basket.vo.TeamMembershipChangeListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BasketTeamMembershipChangeServiceImpl implements IBasketTeamMembershipChangeService {

    private static final String PENDING_ADMIN = "PENDING_ADMIN";
    private static final String INVITE_JOIN = "INVITE_JOIN";
    private static final String APPLY_JOIN = "APPLY_JOIN";
    private static final String CAPTAIN_REMOVE = "CAPTAIN_REMOVE";
    private static final String TEAM_DISSOLVE = "TEAM_DISSOLVE";
    private static final String CAPTAIN_TRANSFER = "CAPTAIN_TRANSFER";
    private static final Integer LEGACY_ACTIVE = 1;

    @Autowired
    private BasketTeamMembershipChangeMapper changeMapper;
    @Autowired
    private BasketTeamMapper teamMapper;
    @Autowired
    private BasketTeamMemberMapper teamMemberMapper;
    @Autowired
    private BasketPlayerProfileMapper playerProfileMapper;
    @Autowired
    private BasketPlayerPlatformAccountMapper platformAccountMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private MembershipRosterApplier rosterApplier;
    @Autowired
    private BasketCaptainGrantMapper captainGrantMapper;
    @Autowired
    private CaptainGrantRuntimeSupport grantRuntimeSupport;
    @Autowired
    private CaptainGrantLogSupport grantLogSupport;

    @Override
    public List<TeamMembershipChangeListVo> selectList(TeamMembershipChangeQuery query) {
        List<TeamMembershipChangeListVo> rows = changeMapper.selectList(query == null ? new TeamMembershipChangeQuery() : query);
        List<Long> playerIds = new ArrayList<>();
        for (TeamMembershipChangeListVo row : rows) {
            fillNames(row);
            if (row.getSubjectPlayerId() != null) {
                playerIds.add(row.getSubjectPlayerId());
            }
        }
        Map<Long, List<com.zhs.basket.domain.BasketPlayerPlatformAccount>> byPlayer = playerIds.isEmpty()
                ? Collections.emptyMap()
                : platformAccountMapper.selectActiveByPlayerIds(playerIds).stream()
                .collect(Collectors.groupingBy(com.zhs.basket.domain.BasketPlayerPlatformAccount::getPlayerId));
        Map<Long, String> platformByDivision = new HashMap<>();
        for (TeamMembershipChangeListVo row : rows) {
            if (row.getSubjectPlayerId() == null) {
                continue;
            }
            String platform = null;
            if (row.getDivisionId() != null) {
                platform = platformByDivision.computeIfAbsent(row.getDivisionId(), divisionId -> {
                    BasketDivision division = divisionMapper.selectById(divisionId);
                    return division == null ? null : division.getPlatformCode();
                });
            }
            row.setGameId(PlatformGameIdDisplay.format(platform, byPlayer.get(row.getSubjectPlayerId())));
        }
        return rows;
    }

    @Override
    public TeamMembershipChangeDetailVo selectDetail(Long id) {
        TeamMembershipChangeDetailVo detail = changeMapper.selectDetail(id);
        if (detail == null) {
            throw new ServiceException("成员变更不存在");
        }
        fillNames(detail);
        fillGap(detail);
        if (detail.getSubjectPlayerId() != null) {
            BasketDivision division = detail.getDivisionId() == null ? null : divisionMapper.selectById(detail.getDivisionId());
            String platform = division == null ? null : division.getPlatformCode();
            detail.setGameId(PlatformGameIdDisplay.format(platform,
                    platformAccountMapper.selectActiveByPlayerIds(Collections.singletonList(detail.getSubjectPlayerId()))));
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void approve(Long id, String auditRemark) {
        BasketTeamMembershipChange change = changeMapper.selectById(id);
        if (change == null) {
            throw new ServiceException("成员变更不存在");
        }
        BasketTeam currentTeam = teamMapper.selectById(change.getTeamId());
        Long currentDivisionId = currentTeam == null ? change.getDivisionId() : currentTeam.getDivisionId();
        lockDivisions(change.getTeamId(), currentDivisionId);
        teamMapper.lockById(change.getTeamId());
        change = changeMapper.selectByIdForUpdate(id);
        if (change == null || !PENDING_ADMIN.equals(change.getChangeStatus())) {
            throw new ServiceException("成员变更已处理，请刷新");
        }
        boolean adding = INVITE_JOIN.equals(change.getChangeType()) || APPLY_JOIN.equals(change.getChangeType());
        boolean dissolving = TEAM_DISSOLVE.equals(change.getChangeType());
        if (!adding && !dissolving && !CAPTAIN_REMOVE.equals(change.getChangeType())) {
            throw new ServiceException("该记录不需要审核通过");
        }
        BasketTeam team = teamMapper.selectById(change.getTeamId());
        if (team == null || !Integer.valueOf(0).equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        if (team.getDivisionId() == null || change.getDivisionId() == null
                || !team.getDivisionId().equals(change.getDivisionId())) {
            throw new ServiceException("这条变更绑定的不是球队当前赛事，不能直接通过。请在当前赛事重新邀请");
        }
        String stage;
        String impact;
        if (dissolving) {
            MembershipRosterEffect effect = dissolveTeam(team, change.getId());
            stage = effect.getStage();
            impact = effect.getImpact();
        } else {
            if (adding) {
                addPermanentMember(team, change);
            } else {
                removePermanentMember(team, change);
            }
            MembershipRosterEffect effect = rosterApplier.apply(team.getId(), change.getSubjectPlayerId(), adding);
            stage = effect.getStage();
            impact = effect.getImpact();
        }
        String remark = StringUtils.isEmpty(auditRemark) ? "审核通过" : auditRemark.trim();
        if (changeMapper.markApproved(id, SecurityUtils.getUserId(), SecurityUtils.getUsername(), remark,
                new Date(), stage, trim(impact)) != 1) {
            throw new ServiceException("成员变更已处理，请刷新");
        }
        syncInvitationStatus(change, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, TeamMembershipChangeRejectReq req) {
        if (req == null || StringUtils.isEmpty(req.getRejectReason())) {
            throw new ServiceException("驳回原因不能为空");
        }
        BasketTeamMembershipChange change = changeMapper.selectByIdForUpdate(id);
        if (change == null || !PENDING_ADMIN.equals(change.getChangeStatus())) {
            throw new ServiceException("成员变更已处理，请刷新");
        }
        if (changeMapper.markRejected(id, SecurityUtils.getUserId(), SecurityUtils.getUsername(),
                req.getRejectReason().trim(), new Date()) != 1) {
            throw new ServiceException("成员变更已处理，请刷新");
        }
        syncInvitationStatus(change, 6);
    }

    @Override
    public TeamCaptainDeskVo loadDesk(String teamNo) {
        if (StringUtils.isEmpty(teamNo)) {
            throw new ServiceException("请填写球队编号");
        }
        BasketTeam team = teamMapper.selectByTeamNo(teamNo.trim());
        if (team == null || !Integer.valueOf(0).equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        TeamCaptainDeskVo vo = new TeamCaptainDeskVo();
        vo.setTeamId(team.getId());
        vo.setTeamNo(team.getTeamNo());
        vo.setTeamName(team.getTeamName());
        vo.setDivisionId(team.getDivisionId());
        vo.setTeamStatus(team.getTeamStatus());
        List<TeamCaptainDeskVo.Member> members = new ArrayList<>();
        for (BasketTeamMember member : teamMemberMapper.selectCurrentByTeamId(team.getId())) {
            TeamCaptainDeskVo.Member item = new TeamCaptainDeskVo.Member();
            item.setTeamMemberId(member.getId());
            item.setMemberId(member.getMemberId());
            item.setPlayerId(member.getPlayerId());
            item.setRoleType(member.getRoleType());
            item.setSeatCaptain(team.getCaptainMemberId() != null
                    && team.getCaptainMemberId().equals(member.getMemberId()));
            BasketPlayerProfile profile = member.getPlayerId() == null ? null : playerProfileMapper.selectById(member.getPlayerId());
            if (profile != null) {
                item.setName(StringUtils.isEmpty(profile.getGameNickname()) ? profile.getPlayerNo() : profile.getGameNickname());
                item.setPlayerNo(profile.getPlayerNo());
            }
            members.add(item);
        }
        vo.setMembers(members);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void transferCaptain(TeamCaptainTransferReq req) {
        if (req == null || req.getTeamId() == null || req.getTeamMemberId() == null) {
            throw new ServiceException("请选择要接手的队员");
        }
        teamMapper.lockById(req.getTeamId());
        BasketTeam team = requireActiveTeam(req.getTeamId());
        BasketTeamMember target = requireCurrentMember(team.getId(), req.getTeamMemberId());
        if (team.getCaptainMemberId() != null && team.getCaptainMemberId().equals(target.getMemberId())) {
            throw new ServiceException("该队员已经是队长");
        }
        BasketPlayerProfile profile = playerProfileMapper.selectById(target.getPlayerId());
        if (profile == null || !Integer.valueOf(0).equals(profile.getIsDelete())) {
            throw new ServiceException("球员不存在");
        }
        if (!Integer.valueOf(3).equals(profile.getCertificationStatus())) {
            throw new ServiceException("接手人尚未认证通过");
        }
        BasketTeamMember previous = currentSeat(team);
        moveGrant(team, previous, target);
        teamMemberMapper.demoteOtherCaptains(team.getId(), target.getId());
        if (teamMemberMapper.updateRole(target.getId(), "CAPTAIN") != 1) {
            throw new ServiceException("队长席位更新失败");
        }
        if (teamMapper.updateCaptain(team.getId(), target.getMemberId(), target.getPlayerId()) != 1) {
            throw new ServiceException("队长席位更新失败");
        }
        String fromName = previous == null ? "原队长" : memberLabel(previous);
        Long initiatorMemberId = previous != null && previous.getMemberId() != null
                ? previous.getMemberId() : target.getMemberId();
        insertChange(team, target, "ADMIN", initiatorMemberId, CAPTAIN_TRANSFER, "EFFECTIVE",
                "队长：" + fromName,
                "队长：" + memberLabel(target),
                "席位和队长授权已交给接手人。原队长仍在队，不能被其他队伍邀请。",
                new Date());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitDissolve(TeamDissolveSubmitReq req) {
        if (req == null || req.getTeamId() == null) {
            throw new ServiceException("请选择球队");
        }
        BasketTeam team = requireActiveTeam(req.getTeamId());
        if (changeMapper.countPendingByTeamAndType(team.getId(), TEAM_DISSOLVE) > 0) {
            throw new ServiceException("已有待审核的解散申请");
        }
        BasketTeamMember seat = currentSeat(team);
        if (seat == null) {
            List<BasketTeamMember> members = teamMemberMapper.selectCurrentByTeamId(team.getId());
            seat = members.isEmpty() ? null : members.get(0);
        }
        if (seat == null || seat.getPlayerId() == null || seat.getMemberId() == null) {
            throw new ServiceException("球队没有可记录的当前成员，不能提交解散");
        }
        String reason = StringUtils.isEmpty(req.getReason()) ? "管理员代提交解散申请" : req.getReason().trim();
        insertChange(team, seat, "ADMIN", seat.getMemberId(), TEAM_DISSOLVE, "PENDING_ADMIN",
                "球队有效，成员仍在队",
                "待管理员审核后解散：" + reason,
                "审核通过前不改变成员关系。通过后全队成员关系结束，这些球员才能被其他队伍邀请。",
                null);
    }

    private MembershipRosterEffect dissolveTeam(BasketTeam team, Long changeId) {
        if (team.getTeamStatus() == null || team.getTeamStatus() != 1) {
            throw new ServiceException("球队已不是可解散状态");
        }
        List<BasketTeamMember> members = teamMemberMapper.selectCurrentByTeamId(team.getId());
        Date now = new Date();
        for (BasketTeamMember member : members) {
            if (teamMemberMapper.endCurrent(member.getId(), 0, now, "管理员审核通过解散球队") != 1) {
                throw new ServiceException("成员状态已变化，请刷新");
            }
            if (member.getPlayerId() != null) {
                playerProfileMapper.clearCurrentTeamIfMatch(member.getPlayerId(), team.getId());
            }
        }
        MembershipRosterEffect released = rosterApplier.releaseTeam(team.getId());
        String stage = released.getStage() == null ? "UNCONTROLLED" : released.getStage();
        StringBuilder impact = new StringBuilder("解散已生效，成员可被其他队伍邀请。");
        if (!StringUtils.isEmpty(released.getImpact())) {
            impact.append(released.getImpact());
        }
        revokeTeamGrants(team.getId(), "球队解散，收回队长授权");
        if (teamMapper.markDissolved(team.getId(), now) != 1) {
            throw new ServiceException("球队状态已变化，请刷新");
        }
        teamMapper.updateMemberCount(team.getId(), 0);
        changeMapper.cancelOpenByTeamExcept(team.getId(), changeId);
        MembershipRosterEffect done = new MembershipRosterEffect();
        done.setStage(stage);
        done.setImpact(impact.toString());
        return done;
    }

    private void moveGrant(BasketTeam team, BasketTeamMember previous, BasketTeamMember target) {
        BasketCaptainGrant targetGrant = captainGrantMapper.selectActiveByDivisionPlayer(team.getDivisionId(), target.getPlayerId());
        if (targetGrant != null && targetGrant.getTeamId() != null && !targetGrant.getTeamId().equals(team.getId())) {
            throw new ServiceException("接手人在本赛事的队长授权已绑定其他球队");
        }
        BasketCaptainGrant previousGrant = previous == null || previous.getPlayerId() == null
                ? null
                : captainGrantMapper.selectActiveByDivisionPlayer(team.getDivisionId(), previous.getPlayerId());
        for (BasketCaptainGrant grant : captainGrantMapper.selectActiveByTeamId(team.getId())) {
            if (targetGrant != null && grant.getId().equals(targetGrant.getId())) {
                continue;
            }
            revokeGrant(grant, "队长移交，收回原授权");
        }
        if (previousGrant != null && (targetGrant == null || !previousGrant.getId().equals(targetGrant.getId()))) {
            BasketCaptainGrant still = captainGrantMapper.selectById(previousGrant.getId());
            if (still != null && CaptainGrantStatusEnum.ACTIVE.getCode().equals(still.getGrantStatus())) {
                revokeGrant(still, "队长移交，收回原授权");
            }
        }
        if (targetGrant == null) {
            Date now = new Date();
            Date validFrom = previousGrant != null && previousGrant.getValidFrom() != null ? previousGrant.getValidFrom() : now;
            Date validTo = previousGrant != null && previousGrant.getValidTo() != null
                    ? previousGrant.getValidTo()
                    : grantRuntimeSupport.resolveValidTo(team.getDivisionId());
            BasketDivision division = divisionMapper.selectById(team.getDivisionId());
            if (division == null || division.getSeasonId() == null) {
                throw new ServiceException("赛事不存在，不能移交队长授权");
            }
            BasketCaptainGrant created = new BasketCaptainGrant();
            created.setDivisionId(team.getDivisionId());
            created.setSeasonId(division.getSeasonId());
            created.setTeamId(team.getId());
            created.setPlayerId(target.getPlayerId());
            created.setMemberId(target.getMemberId());
            created.setGrantStatus(CaptainGrantStatusEnum.ACTIVE.getCode());
            created.setActiveUniqueKey(CaptainGrantActiveKeySupport.buildActiveUniqueKey(team.getDivisionId(), target.getPlayerId()));
            created.setValidFrom(validFrom);
            created.setValidTo(validTo);
            created.setValidToMode(previousGrant != null && previousGrant.getValidToMode() != null
                    ? previousGrant.getValidToMode()
                    : CaptainGrantValidToModeEnum.AUTO_EVENT.getCode());
            created.setGrantedBy(SecurityUtils.getUserId());
            created.setRemark("队长移交");
            created.setGrantSource("MANUAL");
            created.setCreateTime(now);
            created.setUpdateTime(now);
            created.setIsDelete(0);
            if (captainGrantMapper.insert(created) != 1) {
                throw new ServiceException("写入队长授权失败");
            }
            grantLogSupport.writeGrantLog(created, CaptainGrantLogActionTypeEnum.GRANT,
                    null, LEGACY_ACTIVE, "队长移交", "队长移交", "ADMIN");
            return;
        }
        if (targetGrant.getTeamId() == null && captainGrantMapper.bindTeamById(targetGrant.getId(), team.getId()) != 1) {
            throw new ServiceException("接手人的队长授权绑定球队失败");
        }
    }

    private void revokeTeamGrants(Long teamId, String reason) {
        for (BasketCaptainGrant grant : captainGrantMapper.selectActiveByTeamId(teamId)) {
            revokeGrant(grant, reason);
        }
    }

    private void revokeGrant(BasketCaptainGrant grant, String reason) {
        if (captainGrantMapper.revokeById(grant.getId()) != 1) {
            return;
        }
        grantLogSupport.writeGrantLog(grant, CaptainGrantLogActionTypeEnum.REVOKE,
                LEGACY_ACTIVE, 2, reason, grant.getRemark(), "ADMIN");
    }

    private void insertChange(BasketTeam team, BasketTeamMember subject, String initiatorRole, Long initiatorMemberId,
                              String changeType, String status, String before, String after, String impact, Date effectiveTime) {
        Date now = new Date();
        BasketTeamMembershipChange row = new BasketTeamMembershipChange();
        row.setChangeNo("MC" + UUID.randomUUID().toString().replace("-", ""));
        row.setTeamId(team.getId());
        row.setDivisionId(team.getDivisionId());
        row.setChangeType(changeType);
        if (subject == null || subject.getPlayerId() == null || subject.getMemberId() == null) {
            throw new ServiceException("成员变更缺少球员");
        }
        if (initiatorMemberId == null) {
            throw new ServiceException("成员变更缺少发起人");
        }
        row.setSubjectPlayerId(subject.getPlayerId());
        row.setSubjectMemberId(subject.getMemberId());
        row.setInitiatorMemberId(initiatorMemberId);
        row.setInitiatorRole(initiatorRole);
        row.setSourceBizType("TEAM");
        row.setSourceBizId(team.getId());
        row.setBeforeSummary(trim(before));
        row.setAfterSummary(trim(after));
        row.setChangeStatus(status);
        row.setImpactSummary(trim(impact));
        row.setEffectiveTime(effectiveTime);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(0);
        for (BasketRegistration registration : registrationMapper.selectApprovedByTeamId(team.getId())) {
            if (team.getDivisionId() != null && team.getDivisionId().equals(registration.getDivisionId())) {
                row.setRegistrationId(registration.getId());
                break;
            }
        }
        if (changeMapper.insert(row) != 1) {
            throw new ServiceException("写入成员变更失败");
        }
    }

    private BasketTeam requireActiveTeam(Long teamId) {
        BasketTeam team = teamMapper.selectById(teamId);
        if (team == null || !Integer.valueOf(0).equals(team.getIsDelete())) {
            throw new ServiceException("球队不存在");
        }
        if (team.getDivisionId() == null) {
            throw new ServiceException("历史球队不能调整队长");
        }
        if (team.getTeamStatus() == null || team.getTeamStatus() != 1) {
            throw new ServiceException("球队已解散或停用");
        }
        return team;
    }

    private BasketTeamMember requireCurrentMember(Long teamId, Long teamMemberId) {
        for (BasketTeamMember member : teamMemberMapper.selectCurrentByTeamId(teamId)) {
            if (teamMemberId.equals(member.getId())) {
                return member;
            }
        }
        throw new ServiceException("接手人不是这支球队的当前成员");
    }

    private BasketTeamMember currentSeat(BasketTeam team) {
        if (team.getCaptainMemberId() == null) {
            return null;
        }
        for (BasketTeamMember member : teamMemberMapper.selectCurrentByTeamId(team.getId())) {
            if (team.getCaptainMemberId().equals(member.getMemberId())) {
                return member;
            }
        }
        return null;
    }

    private String memberLabel(BasketTeamMember member) {
        if (member == null || member.getPlayerId() == null) {
            return "队员";
        }
        BasketPlayerProfile profile = playerProfileMapper.selectById(member.getPlayerId());
        if (profile == null) {
            return "队员";
        }
        return StringUtils.isEmpty(profile.getGameNickname()) ? profile.getPlayerNo() : profile.getGameNickname();
    }

    private void syncInvitationStatus(BasketTeamMembershipChange change, int status) {
        if (change != null && "INVITATION".equals(change.getSourceBizType()) && change.getSourceBizId() != null) {
            changeMapper.updateInvitationStatus(change.getSourceBizId(), status);
        }
    }

    private void addPermanentMember(BasketTeam team, BasketTeamMembershipChange change) {
        if (teamMemberMapper.selectCurrentByTeamAndPlayer(team.getId(), change.getSubjectPlayerId()) != null) {
            throw new ServiceException("球员已在球队中");
        }
        int current = teamMemberMapper.countCurrentMembers(team.getId());
        if (team.getMaxMemberCount() != null && current >= team.getMaxMemberCount()) {
            throw new ServiceException("球队人数已满");
        }
        if (team.getDivisionId() == null) {
            throw new ServiceException("历史球队不能加入新成员");
        }
        BasketPlayerProfile profile = playerProfileMapper.selectById(change.getSubjectPlayerId());
        if (profile == null || !Integer.valueOf(0).equals(profile.getIsDelete())) {
            throw new ServiceException("球员不存在");
        }
        if (!Integer.valueOf(3).equals(profile.getCertificationStatus())) {
            throw new ServiceException("球员尚未认证通过");
        }
        Date now = new Date();
        BasketTeamMember member = new BasketTeamMember();
        member.setMembershipNo("MB" + UUID.randomUUID().toString().replace("-", ""));
        member.setTeamId(team.getId());
        member.setDivisionId(team.getDivisionId());
        member.setMemberId(change.getSubjectMemberId());
        member.setPlayerId(change.getSubjectPlayerId());
        member.setRoleType("MEMBER");
        member.setRosterType("OFFICIAL");
        member.setJoinSource(APPLY_JOIN.equals(change.getChangeType()) ? "APPLY" : "INVITE");
        member.setMembershipStatus(1);
        member.setIsCurrent(1);
        member.setJoinTime(now);
        member.setIsDelete(0);
        member.setActiveMemberSlot(team.getDivisionId() + "_" + change.getSubjectMemberId());
        member.setCreateTime(now);
        member.setUpdateTime(now);
        try {
            if (teamMemberMapper.insertActive(member) != 1) {
                throw new ServiceException("写入球队成员失败");
            }
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("该球员已在本赛事加入其他球队");
        }
        teamMapper.updateMemberCount(team.getId(), teamMemberMapper.countCurrentMembers(team.getId()));
        profile.setCurrentTeamId(team.getId());
        playerProfileMapper.updateById(profile);
    }

    private void removePermanentMember(BasketTeam team, BasketTeamMembershipChange change) {
        BasketTeamMember member = teamMemberMapper.selectCurrentByTeamAndPlayer(team.getId(), change.getSubjectPlayerId());
        if (member == null) {
            throw new ServiceException("球员已不在球队中");
        }
        if ("CAPTAIN".equals(member.getRoleType())) {
            throw new ServiceException("不能移除队长");
        }
        if (teamMemberMapper.endCurrent(member.getId(), 2, new Date(), "管理员审核通过队长移除") != 1) {
            throw new ServiceException("成员状态已变化，请刷新");
        }
        teamMapper.updateMemberCount(team.getId(), teamMemberMapper.countCurrentMembers(team.getId()));
        playerProfileMapper.clearCurrentTeamIfMatch(change.getSubjectPlayerId(), team.getId());
    }

    private void lockDivisions(Long teamId, Long primaryDivisionId) {
        TreeSet<Long> divisionIds = new TreeSet<>();
        if (primaryDivisionId != null) {
            divisionIds.add(primaryDivisionId);
        }
        BasketTeam team = teamMapper.selectById(teamId);
        Long currentDivisionId = team == null ? null : team.getDivisionId();
        for (BasketRegistration registration : registrationMapper.selectApprovedByTeamId(teamId)) {
            if (currentDivisionId == null || !currentDivisionId.equals(registration.getDivisionId())) {
                continue;
            }
            BasketDivision division = divisionMapper.selectById(registration.getDivisionId());
            if (division == null) {
                continue;
            }
            String status = division.getDivisionStatus();
            if (DivisionStatusEnum.FINISHED.getCode().equals(status)
                    || DivisionStatusEnum.ARCHIVED.getCode().equals(status)) {
                continue;
            }
            divisionIds.add(division.getId());
        }
        for (Long divisionId : divisionIds) {
            divisionMapper.selectByIdForUpdate(divisionId);
        }
    }

    private void fillGap(TeamMembershipChangeDetailVo detail) {
        if (detail.getTeamId() == null) {
            return;
        }
        int permanent = teamMemberMapper.countCurrentMembers(detail.getTeamId());
        detail.setPermanentMemberCount(permanent);
        int pendingAdd = changeMapper.countPendingByTeamAndType(detail.getTeamId(), INVITE_JOIN)
                + changeMapper.countPendingByTeamAndType(detail.getTeamId(), APPLY_JOIN);
        int pendingRemove = changeMapper.countPendingByTeamAndType(detail.getTeamId(), CAPTAIN_REMOVE);
        detail.setPendingAddCount(pendingAdd);
        detail.setPendingRemoveCount(pendingRemove);
        Integer roster = null;
        if (detail.getRegistrationId() != null) {
            BasketRegistration registration = registrationMapper.selectById(detail.getRegistrationId());
            if (registration != null && "APPROVED".equals(registration.getRegistrationStatus())) {
                roster = registration.getLineupCount();
                detail.setEventRosterCount(roster);
            }
        }
        detail.setRosterGapSummary(gapText(permanent, roster, pendingAdd, pendingRemove));
        if (detail.getCertificationStatus() != null) {
            detail.setCertificationStatusName(Integer.valueOf(3).equals(detail.getCertificationStatus()) ? "已认证" : "未认证");
        }
    }

    static String gapText(int permanent, Integer roster, int pendingAdd, int pendingRemove) {
        if (roster == null) {
            return "当前永久球队成员 " + permanent + " 人。该队没有已通过的本届报名名单。";
        }
        if (permanent == roster && pendingAdd == 0 && pendingRemove == 0) {
            return "当前永久球队成员 " + permanent + " 人，本届有效参赛 " + roster + " 人，两边一致。";
        }
        List<String> parts = new ArrayList<>();
        parts.add("当前永久球队成员 " + permanent + " 人");
        parts.add("本届有效参赛 " + roster + " 人");
        if (pendingAdd > 0) {
            parts.add("待审核新增 " + pendingAdd + " 人");
        }
        if (pendingRemove > 0) {
            parts.add("待审核移除 " + pendingRemove + " 人");
        }
        return String.join("，", parts) + "。永久成员和本届参赛名单不是同一口径。";
    }

    private void fillNames(TeamMembershipChangeListVo row) {
        row.setChangeTypeName(typeName(row.getChangeType()));
        row.setChangeStatusName(statusName(row.getChangeStatus()));
        row.setInitiatorRoleName(roleName(row.getInitiatorRole()));
        row.setEffectStageName(stageName(row.getEffectStage()));
    }

    private String typeName(String type) {
        if (INVITE_JOIN.equals(type)) {
            return "邀请入队";
        }
        if (APPLY_JOIN.equals(type)) {
            return "申请入队";
        }
        if (CAPTAIN_REMOVE.equals(type)) {
            return "队长移除";
        }
        if ("VOLUNTARY_LEAVE".equals(type)) {
            return "主动离队";
        }
        if (TEAM_DISSOLVE.equals(type)) {
            return "解散球队";
        }
        if (CAPTAIN_TRANSFER.equals(type)) {
            return "队长移交";
        }
        return type;
    }

    private String statusName(String status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case "PENDING_USER" -> "待用户确认";
            case "PENDING_CAPTAIN" -> "待队长处理";
            case PENDING_ADMIN -> "待管理员审核";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            case "CANCELLED" -> "已取消";
            case "EFFECTIVE" -> "已生效";
            case "EXPIRED" -> "已过期";
            case "HISTORICAL" -> "历史上线前记录";
            default -> status;
        };
    }

    private String roleName(String role) {
        if ("CAPTAIN".equals(role)) {
            return "队长";
        }
        if ("PLAYER".equals(role)) {
            return "球员";
        }
        if ("ADMIN".equals(role)) {
            return "管理员";
        }
        return role == null ? "-" : role;
    }

    private String stageName(String stage) {
        if (stage == null) {
            return "-";
        }
        return switch (stage) {
            case "OPEN_ROSTER" -> "未锁定名单";
            case "LOCKED_ROSTER" -> "已锁定名单";
            case "IN_SEASON" -> "已开赛";
            case "UNCONTROLLED" -> "未参与受控报名";
            default -> stage;
        };
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.length() <= 500 ? text : text.substring(0, 500);
    }
}
