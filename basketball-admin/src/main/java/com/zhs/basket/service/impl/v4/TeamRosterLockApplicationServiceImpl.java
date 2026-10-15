package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionRosterEntry;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.enums.v4.ParticipationRoleEnum;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.enums.v4.RosterVersionStatusEnum;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.v4.BasketDivisionRosterEntryMapper;
import com.zhs.basket.mapper.v4.BasketDivisionRosterVersionMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.support.v4.CurrentRosterEligibility;
import com.zhs.basket.support.v4.RosterEligibilityQueries;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.basket.service.v4.IBasketV4EventSnapshotService;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.service.v4.IBasketV4RuleVersionService;
import com.zhs.basket.service.v4.IBasketV4StructureVersionService;
import com.zhs.basket.service.v4.ITeamRosterLockApplicationService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.support.v4.version.RuleVersionView;
import com.zhs.basket.vo.v4.RosterVersionHistoryItemVo;
import com.zhs.basket.vo.v4.TeamRosterItemVo;
import com.zhs.basket.vo.v4.TeamRosterLockResultVo;
import com.zhs.basket.vo.v4.TeamRosterVersionDetailVo;
import com.zhs.basket.vo.v4.TeamRosterWorkspaceVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TeamRosterLockApplicationServiceImpl implements ITeamRosterLockApplicationService {

    @Autowired
    private TeamRosterSupport rosterSupport;
    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired
    private RosterEligibilityQueries eligibilityQueries;
    @Autowired
    private BasketTeamMapper teamMapper;
    @Autowired
    private BasketDivisionRosterEntryMapper rosterEntryMapper;
    @Autowired
    private BasketDivisionRosterVersionMapper rosterVersionMapper;
    @Autowired
    private IBasketV4RosterVersionService rosterVersionService;
    @Autowired
    private IBasketV4RuleVersionService ruleVersionService;
    @Autowired
    private IBasketV4EventSnapshotService eventSnapshotService;
    @Autowired
    private IBasketV4StructureVersionService structureVersionService;
    @Autowired
    private BasketBusinessAuditWriter auditWriter;

    @Override
    public TeamRosterWorkspaceVo getWorkspace(Long divisionId) {
        BasketDivision division = rosterSupport.requireDivision(divisionId);
        rosterSupport.requireTeamMode(division);
        return buildWorkspace(division);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamRosterLockResultVo lock(Long divisionId, Long operatorId) {
        BasketDivision division = rosterSupport.lockDivision(divisionId);
        rosterSupport.requireTeamMode(division);
        RuleVersionView ruleVersion = validateRule(division);
        List<BasketRegistration> registrations = registrationMapper.selectByDivisionId(divisionId);
        validateCurrentEligibility(division, registrations);
        validateAllocation(division, registrations);
        validateFormalCapacity(division, registrations);
        validateCandidateOrder(registrations);

        String fingerprint = rosterSupport.fingerprint(registrations);
        BasketDivisionRosterVersion current = rosterVersionService.getCurrentLockedVersion(divisionId);
        if (current != null && fingerprint.equals(current.getAllocationFingerprint())) {
            return toLockResult(current, true);
        }

        BasketDivisionRosterVersion version = createRosterVersion(divisionId, operatorId);
        snapshotRosterEntries(version, registrations);
        createFormalEventTeams(version, registrations);
        persistCounts(version, registrations, fingerprint);
        rosterVersionService.lockVersion(version.getId(), operatorId);
        if (current != null) {
            structureVersionService.invalidateByRosterVersion(current.getId());
        }

        BasketDivisionRosterVersion locked = rosterVersionService.getById(version.getId());
        writeLockAudit(divisionId, locked, current);
        return toLockResult(locked, false);
    }

    @Override
    public TeamRosterVersionDetailVo getVersion(Long divisionId, Long rosterVersionId) {
        if (divisionId == null || rosterVersionId == null) {
            throw new ServiceException("divisionId \u4e0e rosterVersionId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        BasketDivisionRosterVersion version = rosterVersionService.getById(rosterVersionId);
        if (version == null || !divisionId.equals(version.getDivisionId())) {
            throw new ServiceException("\u540d\u5355\u7248\u672c\u4e0d\u5b58\u5728");
        }
        TeamRosterVersionDetailVo vo = new TeamRosterVersionDetailVo();
        vo.setRosterVersionId(version.getId());
        vo.setDivisionId(version.getDivisionId());
        vo.setVersionNo(version.getVersionNo());
        vo.setStatus(version.getStatus());
        vo.setIsCurrent(version.getIsCurrent());
        vo.setFormalCount(version.getFormalCount());
        vo.setCandidateCount(version.getCandidateCount());
        vo.setExcludedCount(version.getExcludedCount());
        vo.setLockedTime(version.getLockedTime());
        vo.setLockedBy(version.getLockedBy());

        Map<Long, String> teamNames = new HashMap<>();
        for (BasketDivisionRosterEntry entry : rosterEntryMapper.selectByRosterVersionId(rosterVersionId)) {
            TeamRosterItemVo item = new TeamRosterItemVo();
            item.setRegistrationId(entry.getRegistrationId());
            item.setTeamId(entry.getTeamId());
            item.setTeamName(resolveTeamName(entry.getTeamId(), teamNames));
            item.setParticipationRole(entry.getParticipationRole());
            item.setCandidateOrder(entry.getCandidateOrder());
            BasketRegistration registration = registrationMapper.selectById(entry.getRegistrationId());
            if (registration != null) {
                item.setAuditStatus(registration.getRegistrationStatus());
                item.setSubmitTime(registration.getSubmitTime());
            }
            if (ParticipationRoleEnum.FORMAL.getCode().equals(entry.getParticipationRole())) {
                vo.getFormalList().add(item);
            } else if (ParticipationRoleEnum.CANDIDATE.getCode().equals(entry.getParticipationRole())) {
                vo.getCandidateList().add(item);
            } else {
                vo.getExcludedList().add(item);
            }
        }
        return vo;
    }

    @Override
    public List<RosterVersionHistoryItemVo> listVersions(Long divisionId) {
        if (divisionId == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        List<RosterVersionHistoryItemVo> result = new ArrayList<>();
        for (BasketDivisionRosterVersion version : rosterVersionMapper.selectByDivisionId(divisionId)) {
            RosterVersionHistoryItemVo item = new RosterVersionHistoryItemVo();
            item.setRosterVersionId(version.getId());
            item.setVersionNo(version.getVersionNo());
            item.setStatus(version.getStatus());
            item.setStatusName(com.zhs.basket.support.v4.V4AdminDisplaySupport.rosterStatusLabel(version.getStatus()));
            item.setIsCurrent(version.getIsCurrent());
            item.setFormalCount(version.getFormalCount());
            item.setCandidateCount(version.getCandidateCount());
            item.setExcludedCount(version.getExcludedCount());
            item.setCreateTime(version.getCreateTime());
            item.setLockedTime(version.getLockedTime());
            item.setLockedBy(version.getLockedBy());
            result.add(item);
        }
        return result;
    }

    private RuleVersionView validateRule(BasketDivision division) {
        RuleVersionView ruleVersion = ruleVersionService.getCurrent(division.getId());
        if (ruleVersion == null || !ruleVersion.isLocked()) {
            throw new ServiceException("\u89c4\u5219\u7248\u672c\u5c1a\u672a\u9501\u5b9a\uff0c\u4e0d\u80fd\u9501\u5b9a\u6700\u7ec8\u540d\u5355");
        }
        return ruleVersion;
    }

    private void validateCurrentEligibility(BasketDivision division, List<BasketRegistration> registrations) {
        int minimum = CurrentRosterEligibility.minimum(division);
        if (minimum <= 0) {
            return;
        }
        for (BasketRegistration row : registrations) {
            if (!rosterSupport.isApproved(row) || rosterSupport.roleOf(row) != ParticipationRoleEnum.FORMAL) {
                continue;
            }
            if (eligibilityQueries.eligibleCount(row.getId(), division.getId()) < minimum) {
                throw new ServiceException("球队当前有效参赛人数不足，不能锁定正式名单");
            }
        }
    }

    private void validateAllocation(BasketDivision division, List<BasketRegistration> registrations) {
        for (BasketRegistration row : registrations) {
            ParticipationRoleEnum role = rosterSupport.roleOf(row);
            boolean approved = rosterSupport.isApproved(row);
            if (approved && role == ParticipationRoleEnum.UNALLOCATED) {
                throw new ServiceException("\u4ecd\u6709\u5ba1\u6838\u901a\u8fc7\u4f46\u672a\u5206\u914d\u7684\u7403\u961f\uff0c\u4e0d\u80fd\u9501\u5b9a\u6700\u7ec8\u540d\u5355");
            }
            if ((role == ParticipationRoleEnum.FORMAL || role == ParticipationRoleEnum.CANDIDATE) && !approved) {
                throw new ServiceException("\u6b63\u5f0f/\u5019\u8865\u7403\u961f\u5fc5\u987b\u6765\u81ea\u5ba1\u6838\u901a\u8fc7\u7684\u62a5\u540d");
            }
            if (RegistrationStatusEnum.REJECTED.getCode().equals(row.getRegistrationStatus())
                    && (role == ParticipationRoleEnum.FORMAL || role == ParticipationRoleEnum.CANDIDATE)) {
                throw new ServiceException("\u5df2\u9a73\u56de\u62a5\u540d\u4e0d\u80fd\u8fdb\u5165\u6b63\u5f0f\u6216\u5019\u8865\u540d\u5355");
            }
            if (role == ParticipationRoleEnum.EXCLUDED && !approved
                    && !RegistrationStatusEnum.REJECTED.getCode().equals(row.getRegistrationStatus())) {
                throw new ServiceException("\u4e0d\u53c2\u8d5b\u5206\u914d\u4e0d\u5408\u6cd5");
            }
        }
    }

    private void validateFormalCapacity(BasketDivision division, List<BasketRegistration> registrations) {
        long formal = registrations.stream()
                .filter(row -> rosterSupport.isApproved(row)
                        && rosterSupport.roleOf(row) == ParticipationRoleEnum.FORMAL)
                .count();
        if (formal < 2) {
            throw new ServiceException("\u81f3\u5c11\u9700\u8981 2 \u652f\u6b63\u5f0f\u53c2\u8d5b\u7403\u961f\u624d\u80fd\u9501\u5b9a\u540d\u5355");
        }
        int cap = rosterSupport.formalCap(division);
        if (formal > cap) {
            throw new ServiceException("\u6b63\u5f0f\u53c2\u8d5b\u7403\u961f\u6570\u91cf\u4e0d\u80fd\u8d85\u8fc7\u540d\u989d " + cap);
        }
    }

    private void validateCandidateOrder(List<BasketRegistration> registrations) {
        List<Integer> orders = registrations.stream()
                .filter(row -> rosterSupport.roleOf(row) == ParticipationRoleEnum.CANDIDATE)
                .map(BasketRegistration::getCandidateOrder)
                .sorted(Comparator.nullsLast(Integer::compareTo))
                .toList();
        for (int i = 0; i < orders.size(); i++) {
            if (!Objects.equals(orders.get(i), i + 1)) {
                throw new ServiceException("\u5019\u8865\u987a\u5e8f\u5fc5\u987b\u4ece 1 \u5f00\u59cb\u8fde\u7eed\u4e14\u552f\u4e00");
            }
        }
    }

    private BasketDivisionRosterVersion createRosterVersion(Long divisionId, Long operatorId) {
        try {
            return rosterVersionService.createVersion(divisionId, operatorId);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("\u540d\u5355\u7248\u672c\u521b\u5efa\u51b2\u7a81\uff0c\u8bf7\u91cd\u8bd5");
        }
    }

    private void snapshotRosterEntries(BasketDivisionRosterVersion version, List<BasketRegistration> registrations) {
        Date now = new Date();
        List<BasketDivisionRosterEntry> entries = new ArrayList<>();
        for (BasketRegistration row : registrations) {
            ParticipationRoleEnum role = rosterSupport.roleOf(row);
            if (!rosterSupport.isApproved(row)) {
                continue;
            }
            if (role == ParticipationRoleEnum.UNALLOCATED) {
                continue;
            }
            BasketDivisionRosterEntry entry = new BasketDivisionRosterEntry();
            entry.setRosterVersionId(version.getId());
            entry.setDivisionId(version.getDivisionId());
            entry.setRegistrationId(row.getId());
            entry.setTeamId(row.getTeamId());
            entry.setParticipationRole(role.getCode());
            entry.setCandidateOrder(role == ParticipationRoleEnum.CANDIDATE ? row.getCandidateOrder() : null);
            entry.setCreateTime(now);
            entry.setUpdateTime(now);
            entry.setIsDelete(0);
            entries.add(entry);
        }
        if (!entries.isEmpty() && rosterEntryMapper.batchInsert(entries) != entries.size()) {
            throw new ServiceException("\u5199\u5165\u540d\u5355\u7248\u672c\u6761\u76ee\u5931\u8d25");
        }
    }

    private void createFormalEventTeams(BasketDivisionRosterVersion version, List<BasketRegistration> registrations) {
        for (BasketRegistration row : registrations) {
            if (rosterSupport.isApproved(row) && rosterSupport.roleOf(row) == ParticipationRoleEnum.FORMAL) {
                eventSnapshotService.generateFromFormalRegistration(row, version.getId());
            }
        }
    }

    private void persistCounts(BasketDivisionRosterVersion version, List<BasketRegistration> registrations,
                               String fingerprint) {
        int formal = 0;
        int candidate = 0;
        int excluded = 0;
        for (BasketRegistration row : registrations) {
            if (!rosterSupport.isApproved(row)) {
                continue;
            }
            ParticipationRoleEnum role = rosterSupport.roleOf(row);
            if (role == ParticipationRoleEnum.FORMAL) {
                formal++;
            } else if (role == ParticipationRoleEnum.CANDIDATE) {
                candidate++;
            } else if (role == ParticipationRoleEnum.EXCLUDED) {
                excluded++;
            }
        }
        version.setFormalCount(formal);
        version.setCandidateCount(candidate);
        version.setExcludedCount(excluded);
        version.setAllocationFingerprint(fingerprint);
        if (rosterVersionMapper.updateById(version) != 1) {
            throw new ServiceException("\u66f4\u65b0\u540d\u5355\u7248\u672c\u7edf\u8ba1\u5931\u8d25");
        }
    }

    private TeamRosterWorkspaceVo buildWorkspace(BasketDivision division) {
        TeamRosterWorkspaceVo vo = new TeamRosterWorkspaceVo();
        vo.setDivisionId(division.getId());
        vo.setDivisionName(division.getDivisionName());
        vo.setRegistrationMode(division.getRegistrationMode());
        vo.setRuleVersion(ruleVersionService.getCurrent(division.getId()));
        vo.setFormalCap(division.getMaxTeamCount());

        BasketDivisionRosterVersion current = rosterVersionService.getCurrentLockedVersion(division.getId());
        if (current != null) {
            vo.setRosterStatus(RosterVersionStatusEnum.LOCKED.getCode());
            vo.setCurrentRosterVersionId(current.getId());
            vo.setCurrentRosterVersionNo(current.getVersionNo());
        } else {
            vo.setRosterStatus("NONE");
        }
        boolean locked = current != null;
        boolean ruleLocked = vo.getRuleVersion() != null && vo.getRuleVersion().isLocked();
        vo.setCanEdit(!locked);
        vo.setCanLock(!locked && ruleLocked);

        Map<Long, String> teamNames = new HashMap<>();
        int approved = 0;
        int formal = 0;
        int candidate = 0;
        int excluded = 0;
        int unallocated = 0;
        for (BasketRegistration row : registrationMapper.selectByDivisionId(division.getId())) {
            TeamRosterItemVo item = toItem(row, teamNames);
            if (RegistrationStatusEnum.REJECTED.getCode().equals(row.getRegistrationStatus())) {
                vo.getRejectedList().add(item);
                continue;
            }
            if (!rosterSupport.isApproved(row)) {
                continue;
            }
            approved++;
            vo.getApprovedList().add(item);
            ParticipationRoleEnum role = rosterSupport.roleOf(row);
            if (role == ParticipationRoleEnum.FORMAL) {
                formal++;
                vo.getFormalList().add(item);
            } else if (role == ParticipationRoleEnum.CANDIDATE) {
                candidate++;
                vo.getCandidateList().add(item);
            } else if (role == ParticipationRoleEnum.EXCLUDED) {
                excluded++;
                vo.getExcludedList().add(item);
            } else {
                unallocated++;
                vo.getUnallocatedList().add(item);
            }
        }
        vo.getCandidateList().sort(Comparator.comparing(TeamRosterItemVo::getCandidateOrder,
                Comparator.nullsLast(Integer::compareTo)));
        vo.setApprovedCount(approved);
        vo.setFormalCount(formal);
        vo.setCandidateCount(candidate);
        vo.setExcludedCount(excluded);
        vo.setUnallocatedCount(unallocated);
        return vo;
    }

    private TeamRosterItemVo toItem(BasketRegistration row, Map<Long, String> teamNames) {
        TeamRosterItemVo item = new TeamRosterItemVo();
        item.setRegistrationId(row.getId());
        item.setTeamId(row.getTeamId());
        item.setTeamName(resolveTeamName(row.getTeamId(), teamNames));
        item.setSubmitTime(row.getSubmitTime());
        item.setAuditStatus(row.getRegistrationStatus());
        item.setParticipationRole(rosterSupport.roleOf(row).getCode());
        item.setCandidateOrder(row.getCandidateOrder());
        return item;
    }

    private String resolveTeamName(Long teamId, Map<Long, String> cache) {
        if (teamId == null) {
            return null;
        }
        return cache.computeIfAbsent(teamId, id -> {
            BasketTeam team = teamMapper.selectById(id);
            return team == null ? null : team.getTeamName();
        });
    }

    private TeamRosterLockResultVo toLockResult(BasketDivisionRosterVersion version, boolean replay) {
        TeamRosterLockResultVo vo = new TeamRosterLockResultVo();
        vo.setRosterVersionId(version.getId());
        vo.setVersionNo(version.getVersionNo());
        vo.setStatus(version.getStatus());
        vo.setFormalCount(version.getFormalCount());
        vo.setCandidateCount(version.getCandidateCount());
        vo.setExcludedCount(version.getExcludedCount());
        vo.setIdempotentReplay(replay);
        return vo;
    }

    private void writeLockAudit(Long divisionId, BasketDivisionRosterVersion locked,
                                BasketDivisionRosterVersion previous) {
        List<AuditFieldInput> fields = new ArrayList<>();
        AuditFieldInput versionField = new AuditFieldInput();
        versionField.setFieldName("rosterVersionId");
        versionField.setFieldLabel("\u540d\u5355\u7248\u672c");
        versionField.setBeforeValue(previous == null ? null : String.valueOf(previous.getId()));
        versionField.setAfterValue(String.valueOf(locked.getId()));
        fields.add(versionField);
        auditWriter.writeAudit("V4_ROSTER", "DIVISION_ROSTER", divisionId, "ROSTER_LOCK",
                "\u9501\u5b9a\u6700\u7ec8\u540d\u5355 V" + locked.getVersionNo(), null, fields);
    }
}
