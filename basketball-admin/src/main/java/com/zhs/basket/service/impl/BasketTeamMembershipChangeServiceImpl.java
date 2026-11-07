package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.BasketTeamMember;
import com.zhs.basket.domain.BasketTeamMembershipChange;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.mapper.BasketPlayerPlatformAccountMapper;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.BasketTeamMemberMapper;
import com.zhs.basket.mapper.BasketTeamMembershipChangeMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.req.TeamMembershipChangeRejectReq;
import com.zhs.basket.query.TeamMembershipChangeQuery;
import com.zhs.basket.service.IBasketTeamMembershipChangeService;
import com.zhs.basket.support.PlatformGameIdDisplay;
import com.zhs.basket.support.v4.MembershipRosterApplier;
import com.zhs.basket.vo.MembershipRosterEffect;
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
        if (!adding && !CAPTAIN_REMOVE.equals(change.getChangeType())) {
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
        if (adding) {
            addPermanentMember(team, change);
        } else {
            removePermanentMember(team, change);
        }
        MembershipRosterEffect effect = rosterApplier.apply(team.getId(), change.getSubjectPlayerId(), adding);
        String remark = StringUtils.isEmpty(auditRemark) ? "审核通过" : auditRemark.trim();
        if (changeMapper.markApproved(id, SecurityUtils.getUserId(), SecurityUtils.getUsername(), remark,
                new Date(), effect.getStage(), trim(effect.getImpact())) != 1) {
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
