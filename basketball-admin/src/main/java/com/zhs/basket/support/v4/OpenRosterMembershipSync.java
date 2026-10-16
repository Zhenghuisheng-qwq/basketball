package com.zhs.basket.support.v4;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketRegistrationAudit;
import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationAuditMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationRosterChangeMapper;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.vo.TeamAuditMemberVo;
import com.zhs.basket.vo.v4.RegistrationGapMemberVo;
import com.zhs.basket.vo.v4.RosterSyncResultVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 最终参赛名单锁定前，把已通过报名的有效阵容同步为当前合法球队成员。
 * 名单已锁定或比赛已不可逆时不做自动增删。
 */
@Component
public class OpenRosterMembershipSync {

    private static final Integer NOT_DELETED = 0;

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
    private com.zhs.basket.mapper.BasketTeamMemberMapper teamMemberMapper;
    @Autowired
    private IBasketV4RosterVersionService rosterVersionService;
    @Autowired
    private RosterEligibilityQueries eligibilityQueries;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;
    @Autowired
    private PreMatchEventPlayerSync eventPlayerSync;

    public boolean autoSyncOpen(Long divisionId) {
        if (divisionId == null || eligibilityQueries.frozen(divisionId)) {
            return false;
        }
        return rosterVersionService.getCurrentLockedVersion(divisionId) == null;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<RosterSyncResultVo> syncDivision(Long divisionId) {
        List<RosterSyncResultVo> results = new ArrayList<>();
        if (!autoSyncOpen(divisionId)) {
            return results;
        }
        divisionMapper.selectByIdForUpdate(divisionId);
        for (BasketRegistration registration : registrationMapper.selectByDivisionId(divisionId)) {
            if (registration == null || !"APPROVED".equals(registration.getRegistrationStatus())) {
                continue;
            }
            results.add(syncRegistration(registration.getId()));
        }
        return results;
    }

    @Transactional(rollbackFor = Exception.class)
    public RosterSyncResultVo syncRegistration(Long registrationId) {
        RosterSyncResultVo result = new RosterSyncResultVo();
        result.setRegistrationId(registrationId);
        BasketRegistration registration = registrationMapper.selectById(registrationId);
        if (registration == null || !NOT_DELETED.equals(registration.getIsDelete())) {
            result.setSkipped(true);
            result.setSkipReason("报名不存在");
            return result;
        }
        result.setTeamId(registration.getTeamId());
        if (!"APPROVED".equals(registration.getRegistrationStatus())) {
            result.setSkipped(true);
            result.setSkipReason("报名尚未审核通过");
            return result;
        }
        if (!autoSyncOpen(registration.getDivisionId())) {
            result.setSkipped(true);
            result.setSkipReason(eligibilityQueries.frozen(registration.getDivisionId())
                    ? "赛事已开赛或已结束，不能自动改名单"
                    : "最终参赛名单已锁定，请走名单变更审核");
            return result;
        }
        divisionMapper.selectByIdForUpdate(registration.getDivisionId());
        registration = registrationMapper.selectById(registrationId);
        BasketDivision division = divisionMapper.selectById(registration.getDivisionId());
        if (division == null) {
            result.setSkipped(true);
            result.setSkipReason("赛区不存在");
            return result;
        }

        List<BasketRegistrationPlayer> existing = registrationPlayerMapper.selectByRegistrationId(registrationId);
        Map<Long, BasketRegistrationPlayer> byPlayer = new LinkedHashMap<>();
        for (BasketRegistrationPlayer player : existing) {
            byPlayer.putIfAbsent(player.getPlayerId(), player);
        }
        List<TeamAuditMemberVo> members = registration.getTeamId() == null
                ? List.of()
                : teamMemberMapper.selectCurrentMembersForAudit(registration.getTeamId());
        int max = division.getLineupMaxCount() == null || division.getLineupMaxCount() <= 0
                ? Integer.MAX_VALUE : division.getLineupMaxCount();

        Set<Long> desired = new HashSet<>();
        List<TeamAuditMemberVo> toAdd = new ArrayList<>();
        int kept = 0;
        for (TeamAuditMemberVo member : members) {
            if (member.getPlayerId() == null) {
                continue;
            }
            String block = blockReason(division, registration, member.getPlayerId());
            if (block != null) {
                result.getBlocked().add(gap(member, block));
                continue;
            }
            if (desired.size() >= max) {
                result.getBlocked().add(gap(member, "超过阵容人数上限 " + max));
                continue;
            }
            desired.add(member.getPlayerId());
            if (byPlayer.containsKey(member.getPlayerId())) {
                kept++;
            } else {
                toAdd.add(member);
            }
        }

        int removed = 0;
        for (BasketRegistrationPlayer player : existing) {
            if (desired.contains(player.getPlayerId())) {
                continue;
            }
            removed += registrationPlayerMapper.softDelete(player.getId());
        }
        Date now = new Date();
        int added = 0;
        int sort = existing.size();
        for (TeamAuditMemberVo member : toAdd) {
            BasketPlayerProfile profile = participationSupport.requireCertifiedPlayer(member.getPlayerId());
            BasketPlayerPlatformAccount account = participationSupport.requirePlatformIdentity(
                    member.getPlayerId(), division.getPlatformCode());
            BasketRegistrationPlayer row = new BasketRegistrationPlayer();
            row.setRegistrationId(registration.getId());
            row.setDivisionId(division.getId());
            row.setSeasonId(registration.getSeasonId());
            row.setTeamId(registration.getTeamId());
            row.setPlayerId(member.getPlayerId());
            row.setMemberId(profile.getMemberId());
            row.setPlatformIdentityId(account.getId());
            row.setPlatformCode(participationSupport.normalizePlatformCode(account.getPlatform()));
            row.setGameIdSnapshot(account.getDisplayGameId());
            row.setPlayerNameSnapshot(profile.getGameNickname() != null ? profile.getGameNickname() : member.getNickname());
            row.setPosition(profile.getPositionCode());
            row.setIsCaptain(member.getPlayerId().equals(registration.getCaptainPlayerId()) ? 1 : 0);
            row.setIsStarter(1);
            row.setSortOrder(sort++);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            registrationPlayerMapper.insert(row);
            added++;
        }

        result.setKept(kept);
        result.setAdded(added);
        result.setRemoved(removed);
        result.setEligibleCount(desired.size());
        expirePendingChanges(registration.getId());
        if (added > 0 || removed > 0) {
            registration.setLineupCount(desired.size());
            registration.setLineupVersion((registration.getLineupVersion() == null ? 0 : registration.getLineupVersion()) + 1);
            registration.setUpdateTime(now);
            registrationMapper.updateById(registration);
            writeAudit(registration, added, removed, desired.size(), now);
            eventPlayerSync.syncWritable(registration);
        }
        return result;
    }

    public String blockReason(BasketDivision division, BasketRegistration registration, Long playerId) {
        try {
            participationSupport.requireTeamMember(registration.getTeamId(), playerId);
            participationSupport.requireCertifiedPlayer(playerId);
            BasketPlayerPlatformAccount account = participationSupport.requirePlatformIdentity(playerId, division.getPlatformCode());
            if (account.getDisplayGameId() == null || account.getDisplayGameId().isBlank()) {
                return "平台游戏 ID 未填写";
            }
        } catch (ServiceException ex) {
            return ex.getMessage();
        }
        if (registrationPlayerMapper.countActivePlayerInDivision(division.getId(), playerId, registration.getId()) > 0) {
            return "该球员已在本赛区其他球队的有效报名中";
        }
        return null;
    }

    private void expirePendingChanges(Long registrationId) {
        Date now = new Date();
        for (Long changeId : rosterChangeMapper.selectPendingIdsByRegistrationId(registrationId)) {
            rosterChangeMapper.markRejected(changeId, null,
                    "最终名单锁定前已按当前球队成员自动同步，原名单变更申请失效", now);
        }
    }

    private void writeAudit(BasketRegistration registration, int added, int removed, int remain, Date now) {
        BasketRegistrationAudit audit = new BasketRegistrationAudit();
        audit.setRegistrationId(registration.getId());
        audit.setDivisionId(registration.getDivisionId());
        audit.setSeasonId(registration.getSeasonId());
        audit.setAuditAction("ROSTER_SYNC");
        audit.setBeforeStatus(registration.getRegistrationStatus());
        audit.setAfterStatus(registration.getRegistrationStatus());
        audit.setAuditRemark("锁定前自动同步：新增 " + added + "，释放 " + removed + "，当前合法参赛 " + remain);
        audit.setAuditUserName("system");
        audit.setAuditTime(now);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        registrationAuditMapper.insert(audit);
    }

    private static RegistrationGapMemberVo gap(TeamAuditMemberVo member, String reason) {
        RegistrationGapMemberVo gap = new RegistrationGapMemberVo();
        gap.setPlayerId(member.getPlayerId());
        gap.setPlayerNo(member.getPlayerNo());
        gap.setNickname(member.getNickname());
        gap.setBlockReason(reason);
        return gap;
    }
}
