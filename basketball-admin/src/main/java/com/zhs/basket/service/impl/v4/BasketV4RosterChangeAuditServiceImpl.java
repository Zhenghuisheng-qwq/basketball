package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketRegistrationAudit;
import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.domain.v4.BasketRegistrationRosterChange;
import com.zhs.basket.domain.v4.BasketRegistrationRosterChangePlayer;
import com.zhs.basket.dto.v4.BasketRegistrationAuditRejectReq;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationAuditMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationRosterChangeMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchPlayerStatMapper;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.support.v4.BasketV4ParticipationSupport;
import com.zhs.basket.support.v4.CurrentDisplayCacheEvictor;
import com.zhs.basket.support.v4.InSeasonEventRosterApply;
import com.zhs.basket.support.v4.PreMatchEventPlayerSync;
import com.zhs.basket.support.v4.RosterChangeWindowSupport;
import com.zhs.basket.vo.v4.BasketRosterChangeListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BasketV4RosterChangeAuditServiceImpl {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketRegistrationRosterChangeMapper rosterChangeMapper;
    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private BasketRegistrationAuditMapper registrationAuditMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketV4MatchPlayerStatMapper matchPlayerStatMapper;
    @Autowired
    private BasketMatchSubmissionPlayerStatMapper submissionPlayerStatMapper;
    @Autowired
    private BasketV4ParticipationSupport participationSupport;
    @Autowired
    private PreMatchEventPlayerSync eventPlayerSync;
    @Autowired
    private InSeasonEventRosterApply inSeasonEventRosterApply;
    @Autowired
    private CurrentDisplayCacheEvictor currentDisplayCacheEvictor;
    @Autowired
    private IBasketV4RosterVersionService rosterVersionService;

    public List<BasketRosterChangeListVo> listPending(Long divisionId) {
        if (divisionId == null) {
            return List.of();
        }
        List<BasketRosterChangeListVo> rows = rosterChangeMapper.selectPendingByDivisionId(divisionId);
        for (BasketRosterChangeListVo row : rows) {
            List<Long> current = new ArrayList<>();
            for (BasketRegistrationPlayer player : registrationPlayerMapper.selectByRegistrationId(row.getRegistrationId())) {
                current.add(player.getPlayerId());
            }
            List<Long> target = new ArrayList<>();
            for (BasketRegistrationRosterChangePlayer player : rosterChangeMapper.selectPlayers(row.getId())) {
                target.add(player.getPlayerId());
            }
            row.setCurrentPlayerIds(current);
            row.setTargetPlayerIds(target);
            row.setAddedNames(difference(target, current));
            row.setRemovedNames(difference(current, target));
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long changeId) {
        BasketRegistrationRosterChange change = requirePending(changeId);
        divisionMapper.selectByIdForUpdate(change.getDivisionId());
        change = requirePending(changeId);
        BasketRegistration registration = requireApproved(change.getRegistrationId());
        int expected = change.getBaseLineupVersion() == null ? 0 : change.getBaseLineupVersion();
        int current = registration.getLineupVersion() == null ? 0 : registration.getLineupVersion();
        if (expected != current) {
            throw new ServiceException("原名单已变化，请让队长重新提交名单变更");
        }
        BasketDivision division = divisionMapper.selectById(change.getDivisionId());
        if (rosterVersionService.getCurrentLockedVersion(change.getDivisionId()) == null) {
            throw new ServiceException("最终名单尚未锁定，当前队员由系统自动同步，不能审核覆盖名单");
        }
        if (division == null
                || com.zhs.basket.enums.v4.DivisionStatusEnum.FINISHED.getCode().equals(division.getDivisionStatus())
                || com.zhs.basket.enums.v4.DivisionStatusEnum.ARCHIVED.getCode().equals(division.getDivisionStatus())) {
            throw new ServiceException("赛区已结束或归档，不能调整参赛名单");
        }
        boolean frozen = RosterChangeWindowSupport.isRosterFrozen(division.getDivisionStatus(),
                matchMapper.countStartedByDivisionId(change.getDivisionId()),
                matchPlayerStatMapper.countByDivisionId(change.getDivisionId())
                        + submissionPlayerStatMapper.countByDivisionId(change.getDivisionId()));

        List<BasketRegistrationRosterChangePlayer> targets = rosterChangeMapper.selectPlayers(changeId);
        if (targets.isEmpty()) {
            throw new ServiceException("目标名单不能为空");
        }
        participationSupport.validateLineupSize(division, targets.size());
        Set<Long> seen = new HashSet<>();
        boolean captainPresent = false;
        for (BasketRegistrationRosterChangePlayer target : targets) {
            if (!seen.add(target.getPlayerId())) {
                throw new ServiceException("阵容中存在重复球员");
            }
            if (Integer.valueOf(1).equals(target.getIsCaptain())) {
                captainPresent = true;
            }
            if (registrationPlayerMapper.countActivePlayerInDivision(division.getId(), target.getPlayerId(), registration.getId()) > 0) {
                throw new ServiceException("球员已在该 Division 参与其他报名");
            }
            participationSupport.requireTeamMember(registration.getTeamId(), target.getPlayerId());
            participationSupport.requireCertifiedPlayer(target.getPlayerId());
            participationSupport.requirePlatformIdentity(target.getPlayerId(), division.getPlatformCode());
        }
        if (!captainPresent) {
            throw new ServiceException("队长必须在报名阵容中");
        }

        registrationPlayerMapper.deleteByRegistrationId(registration.getId());
        Date now = new Date();
        int sort = 0;
        for (BasketRegistrationRosterChangePlayer target : targets) {
            BasketPlayerProfile profile = participationSupport.requireCertifiedPlayer(target.getPlayerId());
            BasketPlayerPlatformAccount account = participationSupport.requirePlatformIdentity(target.getPlayerId(), division.getPlatformCode());
            BasketRegistrationPlayer row = new BasketRegistrationPlayer();
            row.setRegistrationId(registration.getId());
            row.setDivisionId(division.getId());
            row.setSeasonId(registration.getSeasonId());
            row.setTeamId(registration.getTeamId());
            row.setPlayerId(target.getPlayerId());
            row.setMemberId(profile.getMemberId());
            row.setPlatformIdentityId(account.getId());
            row.setPlatformCode(division.getPlatformCode());
            row.setGameIdSnapshot(account.getDisplayGameId());
            row.setPlayerNameSnapshot(profile.getGameNickname());
            row.setPosition(profile.getPositionCode());
            row.setIsCaptain(target.getIsCaptain());
            row.setIsStarter(target.getIsStarter() == null ? 1 : target.getIsStarter());
            row.setSortOrder(sort++);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            row.setIsDelete(NOT_DELETED);
            registrationPlayerMapper.insert(row);
        }
        if (registrationMapper.bumpLineupVersion(registration.getId(), targets.size(), expected) != 1) {
            throw new ServiceException("原名单已变化，请让队长重新提交名单变更");
        }
        if (rosterChangeMapper.markApproved(changeId, SecurityUtils.getUserId(), now) != 1) {
            throw new ServiceException("名单变更已处理，请刷新");
        }
        if (frozen) {
            inSeasonEventRosterApply.apply(registration);
            writeAudit(registration, "赛中名单变更已通过，当前阵容已更新，历史比赛统计保留");
        } else {
            eventPlayerSync.syncWritable(registration);
            writeAudit(registration, "名单变更已通过，有效阵容已替换");
        }
        currentDisplayCacheEvictor.afterSourceTeam(registration.getDivisionId(), registration.getTeamId(), registration.getSeasonId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long changeId, BasketRegistrationAuditRejectReq req) {
        if (req == null || StringUtils.isEmpty(req.getRejectReason())) {
            throw new ServiceException("驳回原因不能为空");
        }
        BasketRegistrationRosterChange change = requirePending(changeId);
        if (rosterChangeMapper.markRejected(changeId, SecurityUtils.getUserId(), req.getRejectReason().trim(), new Date()) != 1) {
            throw new ServiceException("名单变更已处理，请刷新");
        }
        BasketRegistration registration = registrationMapper.selectById(change.getRegistrationId());
        if (registration != null) {
            writeAudit(registration, "名单变更已驳回：" + req.getRejectReason().trim());
        }
    }

    private BasketRegistrationRosterChange requirePending(Long changeId) {
        BasketRegistrationRosterChange change = rosterChangeMapper.selectById(changeId);
        if (change == null) {
            throw new ServiceException("名单变更申请不存在");
        }
        if (!"PENDING".equals(change.getChangeStatus())) {
            throw new ServiceException("名单变更已处理，请刷新");
        }
        return change;
    }

    private BasketRegistration requireApproved(Long registrationId) {
        BasketRegistration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new ServiceException("报名不存在");
        }
        if (!RegistrationStatusEnum.APPROVED.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("原报名已不是通过状态，不能替换名单");
        }
        return registration;
    }

    private void writeAudit(BasketRegistration registration, String remark) {
        Date now = new Date();
        BasketRegistrationAudit audit = new BasketRegistrationAudit();
        audit.setRegistrationId(registration.getId());
        audit.setDivisionId(registration.getDivisionId());
        audit.setSeasonId(registration.getSeasonId());
        audit.setAuditAction("ROSTER_CHANGE");
        audit.setBeforeStatus(RegistrationStatusEnum.APPROVED.getCode());
        audit.setAfterStatus(RegistrationStatusEnum.APPROVED.getCode());
        audit.setAuditUserId(SecurityUtils.getUserId());
        audit.setAuditUserName(SecurityUtils.getUsername());
        audit.setAuditTime(now);
        audit.setAuditRemark(remark);
        audit.setCreateTime(now);
        audit.setUpdateTime(now);
        audit.setIsDelete(NOT_DELETED);
        registrationAuditMapper.insert(audit);
    }

    private List<String> difference(List<Long> left, List<Long> right) {
        List<String> result = new ArrayList<>();
        for (Long id : left) {
            if (!right.contains(id)) {
                result.add(String.valueOf(id));
            }
        }
        return result;
    }
}
