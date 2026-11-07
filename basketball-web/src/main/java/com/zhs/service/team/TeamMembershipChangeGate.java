package com.zhs.service.team;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.dao.BasketTeamMembershipChangeDao;
import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketRegistrationDao;
import com.zhs.dao.v4.BasketRegistrationRosterChangeDao;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamInvitation;
import com.zhs.model.BasketTeamJoinApplication;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.BasketTeamMembershipChange;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketRegistration;
import com.zhs.model.v4.BasketRegistrationRosterChange;
import com.zhs.service.impl.v4.EventRosterMembershipSync;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 已有 APPROVED 报名的球队，成员加入和队长移除先进入管理员审核。
 * 未报名球队保持原流程。主动离队立即结束永久关系，并留下管理员可见记录。
 */
@Component
public class TeamMembershipChangeGate {

    public static final String INVITE_JOIN = "INVITE_JOIN";
    public static final String APPLY_JOIN = "APPLY_JOIN";
    public static final String CAPTAIN_REMOVE = "CAPTAIN_REMOVE";
    public static final String VOLUNTARY_LEAVE = "VOLUNTARY_LEAVE";

    public static final String PENDING_USER = "PENDING_USER";
    public static final String PENDING_CAPTAIN = "PENDING_CAPTAIN";
    public static final String PENDING_ADMIN = "PENDING_ADMIN";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String EFFECTIVE = "EFFECTIVE";

    public static final String ROLE_CAPTAIN = "CAPTAIN";
    public static final String ROLE_PLAYER = "PLAYER";
    public static final String SOURCE_INVITATION = "INVITATION";
    public static final String SOURCE_JOIN = "JOIN_APPLICATION";
    public static final String SOURCE_MEMBER = "TEAM_MEMBER";
    public static final String STAGE_UNCONTROLLED = "UNCONTROLLED";

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketTeamMembershipChangeDao changeDao;
    @Resource
    private BasketRegistrationDao registrationDao;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketRegistrationRosterChangeDao rosterChangeDao;
    @Resource
    private EventRosterMembershipSync eventRosterMembershipSync;

    public boolean shouldHold(BasketTeam team) {
        return !liveRegistrations(team).isEmpty();
    }

    public void trackInvitation(BasketTeamInvitation invitation, BasketTeam team) {
        if (invitation == null || team == null || findBySource(SOURCE_INVITATION, invitation.getId()) != null) {
            return;
        }
        insert(base(team, invitation.getInviteePlayerId(), invitation.getInviteeMemberId(),
                invitation.getInviterMemberId(), ROLE_CAPTAIN, INVITE_JOIN, SOURCE_INVITATION, invitation.getId())
                .status(PENDING_USER)
                .before("不在队")
                .after("待球员接受邀请")
                .impact("邀请尚未接受，不改变永久球队和报名名单")
                .stage(shouldHold(team) ? null : STAGE_UNCONTROLLED));
    }

    public void markInvitationRejected(Long invitationId) {
        move(findBySource(SOURCE_INVITATION, invitationId), PENDING_USER, REJECTED,
                "球员已拒绝邀请", "未入队，报名名单不变");
    }

    public Long boundDivisionId(String sourceType, Long sourceId) {
        BasketTeamMembershipChange row = findBySource(sourceType, sourceId);
        return row == null ? null : row.getDivisionId();
    }

    /**
     * 球队沿用到新赛事时，旧赛事上还没生效的邀请和申请失效。
     * 不改它们原来的赛事归属，也不把它们补进新赛事。
     */
    public void closeOpenActionsForPreviousEvent(Long teamId, Long previousDivisionId) {
        if (teamId == null) {
            return;
        }
        LambdaUpdateWrapper<BasketTeamMembershipChange> update = new LambdaUpdateWrapper<BasketTeamMembershipChange>()
                .eq(BasketTeamMembershipChange::getTeamId, teamId)
                .in(BasketTeamMembershipChange::getChangeStatus, PENDING_USER, PENDING_CAPTAIN, PENDING_ADMIN)
                .eq(BasketTeamMembershipChange::getIsDelete, NOT_DELETED)
                .set(BasketTeamMembershipChange::getChangeStatus, CANCELLED)
                .set(BasketTeamMembershipChange::getAfterSummary, "球队已沿用到新赛事，原操作失效")
                .set(BasketTeamMembershipChange::getImpactSummary, "不影响新赛事阵容，也不改写历史赛事记录")
                .set(BasketTeamMembershipChange::getUpdateTime, new Date());
        if (previousDivisionId != null) {
            update.and(wrapper -> wrapper.eq(BasketTeamMembershipChange::getDivisionId, previousDivisionId)
                    .or().isNull(BasketTeamMembershipChange::getDivisionId));
        }
        changeDao.update(null, update);
    }

    /** 球员已加入本届其他球队。待确认和待管理员审核的邀请都失效，不能再补审进队。 */
    public void invalidateOpenInvitation(Long invitationId, String reason) {
        BasketTeamMembershipChange row = findBySource(SOURCE_INVITATION, invitationId);
        if (row == null) {
            return;
        }
        String status = row.getChangeStatus();
        if (!PENDING_USER.equals(status) && !PENDING_ADMIN.equals(status)) {
            return;
        }
        changeDao.update(null, new LambdaUpdateWrapper<BasketTeamMembershipChange>()
                .eq(BasketTeamMembershipChange::getId, row.getId())
                .eq(BasketTeamMembershipChange::getChangeStatus, status)
                .eq(BasketTeamMembershipChange::getIsDelete, NOT_DELETED)
                .set(BasketTeamMembershipChange::getChangeStatus, CANCELLED)
                .set(BasketTeamMembershipChange::getAfterSummary, trim(reason))
                .set(BasketTeamMembershipChange::getImpactSummary, "已加入本届其他球队，这条邀请不再入队")
                .set(BasketTeamMembershipChange::getUpdateTime, new Date()));
    }

    public void markInvitationPendingAdmin(BasketTeamInvitation invitation, BasketTeam team) {
        assertNoPendingForPlayer(team.getId(), invitation.getInviteePlayerId());
        assertNoBlockingRosterChange(team);
        BasketTeamMembershipChange row = findBySource(SOURCE_INVITATION, invitation.getId());
        if (row == null) {
            insert(base(team, invitation.getInviteePlayerId(), invitation.getInviteeMemberId(),
                    invitation.getInviterMemberId(), ROLE_CAPTAIN, INVITE_JOIN, SOURCE_INVITATION, invitation.getId())
                    .status(PENDING_ADMIN)
                    .before("不在队")
                    .after("球员已接受，待管理员审核后入队")
                    .impact("审核通过前不写入有效赛事名单"));
            return;
        }
        move(row, PENDING_USER, PENDING_ADMIN, "球员已接受，待管理员审核后入队", "审核通过前不写入有效赛事名单");
    }

    public void markInvitationEffective(BasketTeamInvitation invitation, BasketTeam team) {
        BasketTeamMembershipChange row = findBySource(SOURCE_INVITATION, invitation.getId());
        if (row == null) {
            insert(base(team, invitation.getInviteePlayerId(), invitation.getInviteeMemberId(),
                    invitation.getInviterMemberId(), ROLE_CAPTAIN, INVITE_JOIN, SOURCE_INVITATION, invitation.getId())
                    .status(EFFECTIVE)
                    .before("不在队")
                    .after("已加入永久球队")
                    .impact("球队没有进行中的已通过报名，接受后直接入队")
                    .stage(STAGE_UNCONTROLLED)
                    .effectiveNow());
            return;
        }
        move(row, PENDING_USER, EFFECTIVE, "已加入永久球队", "球队没有进行中的已通过报名，接受后直接入队");
        stampEffective(row.getId(), STAGE_UNCONTROLLED);
    }

    public void trackJoinApplication(BasketTeamJoinApplication application, BasketTeam team) {
        if (application == null || team == null || findBySource(SOURCE_JOIN, application.getId()) != null) {
            return;
        }
        insert(base(team, application.getApplicantPlayerId(), application.getApplicantMemberId(),
                application.getApplicantMemberId(), ROLE_PLAYER, APPLY_JOIN, SOURCE_JOIN, application.getId())
                .status(PENDING_CAPTAIN)
                .before("不在队")
                .after("待队长处理加入申请")
                .impact("队长未同意前不改变球队和报名名单")
                .stage(shouldHold(team) ? null : STAGE_UNCONTROLLED));
    }

    public void markJoinClosed(Long applicationId, boolean rejected) {
        move(findBySource(SOURCE_JOIN, applicationId), PENDING_CAPTAIN, rejected ? REJECTED : CANCELLED,
                rejected ? "队长已拒绝申请" : "申请已取消", "未入队，报名名单不变");
    }

    public void markJoinPendingAdmin(BasketTeamJoinApplication application, BasketTeam team, Long captainMemberId) {
        assertNoPendingForPlayer(team.getId(), application.getApplicantPlayerId());
        assertNoBlockingRosterChange(team);
        BasketTeamMembershipChange row = findBySource(SOURCE_JOIN, application.getId());
        if (row == null) {
            insert(base(team, application.getApplicantPlayerId(), application.getApplicantMemberId(),
                    captainMemberId, ROLE_CAPTAIN, APPLY_JOIN, SOURCE_JOIN, application.getId())
                    .status(PENDING_ADMIN)
                    .before("不在队")
                    .after("队长已同意，待管理员审核后入队")
                    .impact("审核通过前不写入有效赛事名单"));
            return;
        }
        row.setInitiatorMemberId(captainMemberId);
        row.setInitiatorRole(ROLE_CAPTAIN);
        changeDao.updateById(row);
        move(row, PENDING_CAPTAIN, PENDING_ADMIN, "队长已同意，待管理员审核后入队", "审核通过前不写入有效赛事名单");
    }

    public void markJoinEffective(BasketTeamJoinApplication application, BasketTeam team, Long captainMemberId) {
        BasketTeamMembershipChange row = findBySource(SOURCE_JOIN, application.getId());
        if (row == null) {
            insert(base(team, application.getApplicantPlayerId(), application.getApplicantMemberId(),
                    captainMemberId, ROLE_CAPTAIN, APPLY_JOIN, SOURCE_JOIN, application.getId())
                    .status(EFFECTIVE)
                    .before("不在队")
                    .after("已加入永久球队")
                    .impact("球队没有进行中的已通过报名，队长同意后直接入队")
                    .stage(STAGE_UNCONTROLLED)
                    .effectiveNow());
            return;
        }
        row.setInitiatorMemberId(captainMemberId);
        row.setInitiatorRole(ROLE_CAPTAIN);
        changeDao.updateById(row);
        move(row, PENDING_CAPTAIN, EFFECTIVE, "已加入永久球队", "球队没有进行中的已通过报名，队长同意后直接入队");
        stampEffective(row.getId(), STAGE_UNCONTROLLED);
    }

    public boolean deferCaptainRemove(BasketTeam team, BasketTeamMember target, Long captainMemberId) {
        if (team == null || target == null || !shouldHold(team)) {
            return false;
        }
        assertNoPendingForPlayer(team.getId(), target.getPlayerId());
        assertNoBlockingRosterChange(team);
        insert(base(team, target.getPlayerId(), target.getMemberId(), captainMemberId, ROLE_CAPTAIN,
                CAPTAIN_REMOVE, SOURCE_MEMBER, target.getId())
                .status(PENDING_ADMIN)
                .before("当前在队")
                .after("待管理员审核后移出")
                .impact("审核通过前不改变永久成员和正式参赛名单"));
        return true;
    }

    public void recordImmediateRemove(BasketTeam team, BasketTeamMember target, Long captainMemberId) {
        insert(base(team, target.getPlayerId(), target.getMemberId(), captainMemberId, ROLE_CAPTAIN,
                CAPTAIN_REMOVE, SOURCE_MEMBER, target.getId())
                .status(EFFECTIVE)
                .before("当前在队")
                .after("已被队长移出")
                .impact("球队没有进行中的已通过报名，移除立即生效")
                .stage(STAGE_UNCONTROLLED)
                .effectiveNow());
    }

    public void recordVoluntaryLeave(BasketTeam team, BasketTeamMember member, String rosterNote) {
        cancelPendingRemove(team.getId(), member.getPlayerId());
        String impact = rosterNote == null || rosterNote.isBlank()
                ? "永久成员关系已结束"
                : rosterNote;
        insert(base(team, member.getPlayerId(), member.getMemberId(), member.getMemberId(), ROLE_PLAYER,
                VOLUNTARY_LEAVE, SOURCE_MEMBER, member.getId())
                .status(EFFECTIVE)
                .before("当前在队")
                .after("已主动离队")
                .impact(trim(impact))
                .stage(shouldHold(team) ? null : STAGE_UNCONTROLLED)
                .effectiveNow());
    }

    public void assertNoPendingAdmin(Long teamId) {
        if (teamId == null) {
            return;
        }
        Long count = changeDao.selectCount(new LambdaQueryWrapper<BasketTeamMembershipChange>()
                .eq(BasketTeamMembershipChange::getTeamId, teamId)
                .eq(BasketTeamMembershipChange::getChangeStatus, PENDING_ADMIN)
                .eq(BasketTeamMembershipChange::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("已有待管理员审核的成员变更，请等待处理后再提交名单变更");
        }
    }

    private void cancelPendingRemove(Long teamId, Long playerId) {
        if (teamId == null || playerId == null) {
            return;
        }
        changeDao.update(null, new LambdaUpdateWrapper<BasketTeamMembershipChange>()
                .eq(BasketTeamMembershipChange::getTeamId, teamId)
                .eq(BasketTeamMembershipChange::getSubjectPlayerId, playerId)
                .eq(BasketTeamMembershipChange::getChangeType, CAPTAIN_REMOVE)
                .eq(BasketTeamMembershipChange::getChangeStatus, PENDING_ADMIN)
                .eq(BasketTeamMembershipChange::getIsDelete, NOT_DELETED)
                .set(BasketTeamMembershipChange::getChangeStatus, CANCELLED)
                .set(BasketTeamMembershipChange::getAuditRemark, "球员已主动离队，移除申请取消")
                .set(BasketTeamMembershipChange::getUpdateTime, new Date()));
    }

    private void assertNoPendingForPlayer(Long teamId, Long playerId) {
        Long count = changeDao.selectCount(new LambdaQueryWrapper<BasketTeamMembershipChange>()
                .eq(BasketTeamMembershipChange::getTeamId, teamId)
                .eq(BasketTeamMembershipChange::getSubjectPlayerId, playerId)
                .eq(BasketTeamMembershipChange::getChangeStatus, PENDING_ADMIN)
                .eq(BasketTeamMembershipChange::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("该球员已有待管理员审核的变更");
        }
    }

    private void assertNoBlockingRosterChange(BasketTeam team) {
        for (BasketRegistration registration : liveRegistrations(team)) {
            if (eventRosterMembershipSync.autoSyncOpen(registration.getDivisionId())) {
                continue;
            }
            Long pending = rosterChangeDao.selectCount(new LambdaQueryWrapper<BasketRegistrationRosterChange>()
                    .eq(BasketRegistrationRosterChange::getRegistrationId, registration.getId())
                    .eq(BasketRegistrationRosterChange::getChangeStatus, "PENDING")
                    .eq(BasketRegistrationRosterChange::getIsDelete, NOT_DELETED));
            if (pending != null && pending > 0) {
                throw new ServiceException("已有待审核的名单变更，请等待处理后再调整成员");
            }
        }
    }

    /**
     * 只认球队当前赛区上还没结束的已通过报名。
     * 继承到新赛事后，旧赛季报名不再挡住入队，也不再把新成员写回旧赛事。
     */
    private List<BasketRegistration> liveRegistrations(BasketTeam team) {
        if (team == null || team.getId() == null || team.getDivisionId() == null) {
            return List.of();
        }
        List<BasketRegistration> registrations = registrationDao.selectList(new LambdaQueryWrapper<BasketRegistration>()
                .eq(BasketRegistration::getTeamId, team.getId())
                .eq(BasketRegistration::getDivisionId, team.getDivisionId())
                .eq(BasketRegistration::getRegistrationStatus, "APPROVED")
                .eq(BasketRegistration::getIsDelete, NOT_DELETED));
        return registrations.stream().filter(registration -> {
            BasketDivision division = divisionDao.selectById(registration.getDivisionId());
            if (division == null) {
                return false;
            }
            String status = division.getDivisionStatus();
            return !"FINISHED".equals(status) && !"ARCHIVED".equals(status);
        }).toList();
    }

    private BasketTeamMembershipChange findBySource(String sourceType, Long sourceId) {
        if (sourceId == null) {
            return null;
        }
        return changeDao.selectOne(new LambdaQueryWrapper<BasketTeamMembershipChange>()
                .eq(BasketTeamMembershipChange::getSourceBizType, sourceType)
                .eq(BasketTeamMembershipChange::getSourceBizId, sourceId)
                .eq(BasketTeamMembershipChange::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTeamMembershipChange::getId)
                .last("limit 1"));
    }

    private void move(BasketTeamMembershipChange row, String from, String to, String after, String impact) {
        if (row == null) {
            return;
        }
        int updated = changeDao.update(null, new LambdaUpdateWrapper<BasketTeamMembershipChange>()
                .eq(BasketTeamMembershipChange::getId, row.getId())
                .eq(BasketTeamMembershipChange::getChangeStatus, from)
                .eq(BasketTeamMembershipChange::getIsDelete, NOT_DELETED)
                .set(BasketTeamMembershipChange::getChangeStatus, to)
                .set(BasketTeamMembershipChange::getAfterSummary, trim(after))
                .set(BasketTeamMembershipChange::getImpactSummary, trim(impact))
                .set(BasketTeamMembershipChange::getUpdateTime, new Date()));
        if (updated == 0 && !to.equals(row.getChangeStatus())) {
            throw new ServiceException("成员变更状态已变化，请刷新后重试");
        }
    }

    private void stampEffective(Long id, String stage) {
        Date now = new Date();
        changeDao.update(null, new LambdaUpdateWrapper<BasketTeamMembershipChange>()
                .eq(BasketTeamMembershipChange::getId, id)
                .set(BasketTeamMembershipChange::getEffectStage, stage)
                .set(BasketTeamMembershipChange::getEffectiveTime, now)
                .set(BasketTeamMembershipChange::getUpdateTime, now));
    }

    private Draft base(BasketTeam team, Long playerId, Long memberId, Long initiatorId, String initiatorRole,
                       String changeType, String sourceType, Long sourceId) {
        Draft draft = new Draft();
        draft.teamId = team.getId();
        draft.divisionId = team.getDivisionId();
        draft.playerId = playerId;
        draft.memberId = memberId;
        draft.initiatorId = initiatorId;
        draft.initiatorRole = initiatorRole;
        draft.changeType = changeType;
        draft.sourceType = sourceType;
        draft.sourceId = sourceId;
        for (BasketRegistration registration : liveRegistrations(team)) {
            draft.registrationId = registration.getId();
            break;
        }
        return draft;
    }

    private void insert(Draft draft) {
        Date now = new Date();
        BasketTeamMembershipChange row = new BasketTeamMembershipChange();
        row.setChangeNo("MC" + IdUtil.getSnowflakeNextIdStr());
        row.setTeamId(draft.teamId);
        row.setDivisionId(draft.divisionId);
        row.setRegistrationId(draft.registrationId);
        row.setChangeType(draft.changeType);
        row.setSubjectPlayerId(draft.playerId);
        row.setSubjectMemberId(draft.memberId);
        row.setInitiatorMemberId(draft.initiatorId);
        row.setInitiatorRole(draft.initiatorRole);
        row.setSourceBizType(draft.sourceType);
        row.setSourceBizId(draft.sourceId);
        row.setBeforeSummary(trim(draft.before));
        row.setAfterSummary(trim(draft.after));
        row.setChangeStatus(draft.status);
        row.setEffectStage(draft.stage);
        row.setImpactSummary(trim(draft.impact));
        row.setEffectiveTime(draft.effectiveNow ? now : null);
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(NOT_DELETED);
        changeDao.insert(row);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.length() <= 500 ? text : text.substring(0, 500);
    }

    private static final class Draft {
        private Long teamId;
        private Long divisionId;
        private Long registrationId;
        private Long playerId;
        private Long memberId;
        private Long initiatorId;
        private String initiatorRole;
        private String changeType;
        private String sourceType;
        private Long sourceId;
        private String status;
        private String before;
        private String after;
        private String impact;
        private String stage;
        private boolean effectiveNow;

        private Draft status(String value) {
            this.status = value;
            return this;
        }

        private Draft before(String value) {
            this.before = value;
            return this;
        }

        private Draft after(String value) {
            this.after = value;
            return this;
        }

        private Draft impact(String value) {
            this.impact = value;
            return this;
        }

        private Draft stage(String value) {
            this.stage = value;
            return this;
        }

        private Draft effectiveNow() {
            this.effectiveNow = true;
            return this;
        }
    }
}
