package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTeamSeasonProfile;
import com.zhs.basket.domain.BasketTournamentRosterChangeRecord;
import com.zhs.basket.domain.BasketTournamentRosterOperationLog;
import com.zhs.basket.domain.BasketTournamentSeasonRoster;
import com.zhs.basket.enums.RosterLockStatusEnum;
import com.zhs.basket.enums.SeasonRosterStatusEnum;
import com.zhs.basket.mapper.BasketTeamSeasonProfileMapper;
import com.zhs.basket.mapper.BasketTournamentRosterChangeRecordMapper;
import com.zhs.basket.mapper.BasketTournamentRosterOperationLogMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonRosterMapper;
import com.zhs.basket.query.RosterOperationLogQuery;
import com.zhs.basket.req.RosterOperationForceRemoveReq;
import com.zhs.basket.req.RosterOperationUpdateRoleReq;
import com.zhs.basket.service.IBasketRosterOperationService;
import com.zhs.basket.vo.RosterOperationLogVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class BasketRosterOperationServiceImpl implements IBasketRosterOperationService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentSeasonRosterMapper seasonRosterMapper;
    @Autowired private BasketTournamentRosterOperationLogMapper operationLogMapper;
    @Autowired private BasketTournamentRosterChangeRecordMapper changeRecordMapper;
    @Autowired private BasketTeamSeasonProfileMapper teamSeasonProfileMapper;

    @Override
    public List<RosterOperationLogVo> selectLogs(RosterOperationLogQuery query) {
        return operationLogMapper.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceRemove(RosterOperationForceRemoveReq req) {
        requireReason(req != null ? req.getOperateReason() : null);
        BasketTournamentSeasonRoster roster = requireActiveRoster(req.getRosterId());
        Date now = new Date();
        seasonRosterMapper.invalidateActiveRoster(roster.getId(), SeasonRosterStatusEnum.REMOVED.getCode(), now);
        writeOperationLog(roster, "FORCE_REMOVE", roster.getRosterRole(), "REMOVED", req.getOperateReason(), now);
        writeChangeRecord(roster, "ADMIN_FORCE_REMOVE", req.getOperateReason(), now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RosterOperationUpdateRoleReq req) {
        requireReason(req != null ? req.getOperateReason() : null);
        BasketTournamentSeasonRoster roster = requireActiveRoster(req.getRosterId());
        String before = roster.getRosterRole() + "/" + roster.getIsStarting() + "/" + roster.getIsSubstitute();
        roster.setRosterRole(req.getRosterRole());
        roster.setIsStarting(req.getIsStarting());
        roster.setIsSubstitute(req.getIsSubstitute());
        roster.setUpdateTime(new Date());
        seasonRosterMapper.updateRoleFields(roster.getId(), roster.getRosterRole(), roster.getIsStarting(), roster.getIsSubstitute());
        Date now = new Date();
        String after = roster.getRosterRole() + "/" + roster.getIsStarting() + "/" + roster.getIsSubstitute();
        writeOperationLog(roster, "UPDATE_ROLE", before, after, req.getOperateReason(), now);
        writeChangeRecord(roster, "ADMIN_UPDATE_ROLE", req.getOperateReason(), now);
    }

    private BasketTournamentSeasonRoster requireActiveRoster(Long rosterId) {
        if (rosterId == null) throw new ServiceException("阵容主键不能为空");
        BasketTournamentSeasonRoster roster = seasonRosterMapper.selectById(rosterId);
        if (roster == null || !Objects.equals(roster.getRosterStatus(), SeasonRosterStatusEnum.ACTIVE.getCode())) {
            throw new ServiceException("阵容不存在或未生效");
        }
        if (roster.getEventTeamId() != null) {
            BasketTeamSeasonProfile profile = teamSeasonProfileMapper.selectById(roster.getEventTeamId());
            if (profile != null && Objects.equals(profile.getRosterLockStatus(), RosterLockStatusEnum.LOCKED.getCode())) {
                throw new ServiceException("赛事球队阵容已锁定，无法操作");
            }
        }
        return roster;
    }

    private void requireReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new ServiceException("操作原因不能为空");
        }
    }

    private void writeOperationLog(BasketTournamentSeasonRoster roster, String operationType, String before,
            String after, String reason, Date now) {
        BasketTournamentRosterOperationLog log = new BasketTournamentRosterOperationLog();
        log.setSeasonId(roster.getSeasonId());
        log.setEventTeamId(roster.getEventTeamId());
        log.setOperationType(operationType);
        log.setTargetRosterId(roster.getId());
        log.setTargetPlayerId(roster.getPlayerId());
        log.setBeforeValue(before);
        log.setAfterValue(after);
        log.setOperateReason(reason);
        log.setEffectiveTime(now);
        log.setOperatorId(SecurityUtils.getUserId());
        log.setOperatorName(SecurityUtils.getUsername());
        log.setCreateTime(now);
        log.setUpdateTime(now);
        log.setIsDelete(NOT_DELETED);
        operationLogMapper.insert(log);
    }

    private void writeChangeRecord(BasketTournamentSeasonRoster roster, String changeType, String reason, Date now) {
        BasketTournamentRosterChangeRecord record = new BasketTournamentRosterChangeRecord();
        record.setSeasonId(roster.getSeasonId());
        record.setChangeType(changeType);
        record.setPlayerId(roster.getPlayerId());
        record.setSourceEventTeamId(roster.getEventTeamId());
        record.setSourceRosterId(roster.getId());
        record.setBeforeSnapshot(roster.getRosterRole());
        record.setAfterSnapshot(changeType);
        record.setOperatorId(SecurityUtils.getUserId());
        record.setOperatorName(SecurityUtils.getUsername());
        record.setOperateReason(reason);
        record.setEffectiveTime(now);
        record.setCreateTime(now);
        record.setUpdateTime(now);
        record.setIsDelete(NOT_DELETED);
        changeRecordMapper.insert(record);
    }
}
