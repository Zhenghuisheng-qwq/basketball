package com.zhs.basket.support.v4;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketRegistrationAudit;
import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationAuditMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationRosterChangeMapper;
import com.zhs.basket.vo.MembershipRosterEffect;
import com.zhs.basket.vo.v4.RegistrationGapMemberVo;
import com.zhs.basket.vo.v4.RosterSyncResultVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 审核通过后，按报名所处阶段更新当前合法名单。不覆盖历史比赛和统计。
 */
@Component
public class MembershipRosterApplier {

    private static final Integer NOT_DELETED = 0;
    public static final String OPEN_ROSTER = "OPEN_ROSTER";
    public static final String LOCKED_ROSTER = "LOCKED_ROSTER";
    public static final String IN_SEASON = "IN_SEASON";

    @Autowired
    private BasketTeamMapper teamMapper;
    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private BasketRegistrationAuditMapper registrationAuditMapper;
    @Autowired
    private BasketRegistrationRosterChangeMapper rosterChangeMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private OpenRosterMembershipSync openRosterMembershipSync;
    @Autowired
    private PreMatchEventPlayerSync preMatchEventPlayerSync;
    @Autowired
    private InSeasonEventRosterApply inSeasonEventRosterApply;
    @Autowired
    private CurrentDisplayCacheEvictor currentDisplayCacheEvictor;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;
    @Autowired
    private RosterEligibilityQueries eligibilityQueries;

    public MembershipRosterEffect apply(Long teamId, Long playerId, boolean adding) {
        MembershipRosterEffect effect = new MembershipRosterEffect();
        effect.setStage(OPEN_ROSTER);
        StringBuilder impact = new StringBuilder();
        boolean any = false;
        boolean inSeason = false;
        boolean locked = false;
        BasketTeam team = teamMapper.selectById(teamId);
        Long currentDivisionId = team == null ? null : team.getDivisionId();
        List<BasketRegistration> registrations = registrationMapper.selectApprovedByTeamId(teamId);
        for (BasketRegistration registration : registrations) {
            if (currentDivisionId == null || !currentDivisionId.equals(registration.getDivisionId())) {
                continue;
            }
            BasketDivision division = divisionMapper.selectById(registration.getDivisionId());
            if (division == null || finished(division)) {
                continue;
            }
            any = true;
            if (openRosterMembershipSync.autoSyncOpen(division.getId())) {
                RosterSyncResultVo synced = openRosterMembershipSync.syncRegistration(registration.getId());
                if (synced.isSkipped()) {
                    throw new ServiceException(synced.getSkipReason() == null ? "未锁定名单未能同步" : synced.getSkipReason());
                }
                if (adding) {
                    for (RegistrationGapMemberVo blocked : synced.getBlocked()) {
                        if (playerId.equals(blocked.getPlayerId())) {
                            throw new ServiceException(blocked.getBlockReason() == null
                                    ? "球员未满足赛事资格" : blocked.getBlockReason());
                        }
                    }
                }
                impact.append("报名").append(registration.getId()).append("未锁定，已按当前合法成员同步；");
                continue;
            }
            if (!rosterChangeMapper.selectPendingIdsByRegistrationId(registration.getId()).isEmpty()) {
                throw new ServiceException("已有待审核的名单变更，请先处理后再审核成员变更");
            }
            boolean frozen = eligibilityQueries.frozen(division.getId());
            boolean changed = delta(registration, division, playerId, adding);
            if (frozen) {
                inSeason = true;
                if (changed) {
                    inSeasonEventRosterApply.apply(registrationMapper.selectById(registration.getId()));
                    currentDisplayCacheEvictor.afterSourceTeam(registration.getDivisionId(), teamId, registration.getSeasonId());
                }
                impact.append("报名").append(registration.getId()).append("已开赛，仅调整后续出场资格，历史统计保留；");
            } else {
                locked = true;
                if (changed) {
                    preMatchEventPlayerSync.syncWritable(registrationMapper.selectById(registration.getId()));
                    currentDisplayCacheEvictor.afterSourceTeam(registration.getDivisionId(), teamId, registration.getSeasonId());
                }
                impact.append("报名").append(registration.getId()).append("名单已锁定，审核通过后更新当前参赛名单；");
            }
        }
        if (!any) {
            effect.setStage("UNCONTROLLED");
            effect.setImpact("没有进行中的已通过报名");
            return effect;
        }
        if (inSeason) {
            effect.setStage(IN_SEASON);
        } else if (locked) {
            effect.setStage(LOCKED_ROSTER);
        } else {
            effect.setStage(OPEN_ROSTER);
        }
        effect.setImpact(impact.toString());
        return effect;
    }

    private boolean delta(BasketRegistration registration, BasketDivision division, Long playerId, boolean adding) {
        List<BasketRegistrationPlayer> existing = registrationPlayerMapper.selectByRegistrationId(registration.getId());
        BasketRegistrationPlayer current = null;
        for (BasketRegistrationPlayer player : existing) {
            if (playerId.equals(player.getPlayerId())) {
                current = player;
                break;
            }
        }
        int size = existing.size();
        Date now = new Date();
        if (adding) {
            if (current != null) {
                return false;
            }
            if (registrationPlayerMapper.countActivePlayerInDivision(division.getId(), playerId, registration.getId()) > 0) {
                throw new ServiceException("球员已在该 Division 参与其他报名");
            }
            BasketPlayerProfile profile = participationSupport.requireCertifiedPlayer(playerId);
            BasketPlayerPlatformAccount account = participationSupport.requirePlatformIdentity(playerId, division.getPlatformCode());
            participationSupport.requireTeamMember(registration.getTeamId(), playerId);
            int max = division.getLineupMaxCount() == null || division.getLineupMaxCount() <= 0
                    ? Integer.MAX_VALUE : division.getLineupMaxCount();
            if (size + 1 > max) {
                throw new ServiceException("超过阵容人数上限 " + max);
            }
            BasketRegistrationPlayer row = new BasketRegistrationPlayer();
            row.setRegistrationId(registration.getId());
            row.setDivisionId(division.getId());
            row.setSeasonId(registration.getSeasonId());
            row.setTeamId(registration.getTeamId());
            row.setPlayerId(playerId);
            row.setMemberId(profile.getMemberId());
            row.setPlatformIdentityId(account.getId());
            row.setPlatformCode(participationSupport.normalizePlatformCode(account.getPlatform()));
            row.setGameIdSnapshot(account.getDisplayGameId());
            row.setPlayerNameSnapshot(participationSupport.snapshotName(profile));
            row.setPosition(profile.getPositionCode());
            row.setIsCaptain(playerId.equals(registration.getCaptainPlayerId()) ? 1 : 0);
            row.setIsStarter(1);
            row.setSortOrder(size);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            registrationPlayerMapper.insert(row);
            size = size + 1;
        } else {
            if (current == null) {
                return false;
            }
            if (Integer.valueOf(1).equals(current.getIsCaptain())) {
                throw new ServiceException("队长必须留在报名阵容中");
            }
            int min = division.getLineupMinCount() == null ? 0 : division.getLineupMinCount();
            if (min > 0 && size - 1 < min) {
                throw new ServiceException("移除后低于阵容人数下限 " + min);
            }
            if (registrationPlayerMapper.softDelete(current.getId()) != 1) {
                throw new ServiceException("报名名单已变化，请刷新后重试");
            }
            size = size - 1;
        }
        int expected = registration.getLineupVersion() == null ? 0 : registration.getLineupVersion();
        if (registrationMapper.bumpLineupVersion(registration.getId(), size, expected) != 1) {
            throw new ServiceException("原名单已变化，请刷新后重试");
        }
        writeAudit(registration, adding ? "成员变更审核通过，已加入当前报名名单" : "成员变更审核通过，已移出当前报名名单");
        return true;
    }

    private boolean finished(BasketDivision division) {
        String status = division.getDivisionStatus();
        return DivisionStatusEnum.FINISHED.getCode().equals(status)
                || DivisionStatusEnum.ARCHIVED.getCode().equals(status);
    }

    private void writeAudit(BasketRegistration registration, String remark) {
        Date now = new Date();
        BasketRegistrationAudit audit = new BasketRegistrationAudit();
        audit.setRegistrationId(registration.getId());
        audit.setDivisionId(registration.getDivisionId());
        audit.setSeasonId(registration.getSeasonId());
        audit.setAuditAction("ROSTER_CHANGE");
        audit.setBeforeStatus("APPROVED");
        audit.setAfterStatus("APPROVED");
        audit.setAuditUserId(SecurityUtils.getUserId());
        audit.setAuditUserName(SecurityUtils.getUsername());
        audit.setAuditTime(now);
        audit.setAuditRemark(remark);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        registrationAuditMapper.insert(audit);
    }
}
